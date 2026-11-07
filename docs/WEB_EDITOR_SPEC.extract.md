# WEB_EDITOR_SPEC.extract.md —— 关键公式伪代码清单

> 配套 `WEB_EDITOR_SPEC.md` 使用；本文件把最容易做错、必须逐行照抄的几何/布局/交互公式整理成**语言无关伪代码**。
> 来源全部为 `js/render.js`、`js/state.js`、`js/ui.js`、`js/interact.js`；行号以此为准。
> 记号：`h`=行高 `sign.rowHeight`；`s`=元素内容尺寸；`m`=文本测量器；`m(t,f,w,px)`=整串前进宽；`m.ink(t,f,w,px)→{abl,abr,adv}`；`m.ascent/m.descent`=墨区上伸/下伸；`ceil/floor/round` 为取整；所有除法为浮点除法（除非注明）。
> **总原则：先算 layout（纯几何），再绘制；同一元素只测量一次；所有坐标只有一套来源。**（`render.js:2-14`）
>
> 校验说明：本文件的伪代码已逐函数实现并与源码 `render.js` 的 metrics 输出做过对照（在 Node 中加载 `core/state/render.js` + 合成文本测量器，覆盖双语/小号双语/大文本/数字线路/文本线路/出入口/出口/8 向箭头/space/icon/layout 共 83 组用例，全部一致）；`state.js` 的 paddingAuto、moveElements、moveElementsBy、sanitize/序列化/剪贴板行为也做了行为对照。

---

## 1. 文本测量器接口（移植核心）

```
// core.js:342-382 —— 浏览器实现；Java 端必须提供等价语义
measure(text, family, weight, sizePx):
    if text == "" return 0
    // 等价：ctx.font = weight + " " + sizePx + "px " + family; ceil(ctx.measureText(text).width)
    return ceil(stringAdvance(text, family, weight, sizePx))     // 整串，含 kerning

measure.ascent(text, family, weight, sizePx):
    if text == "" return 0
    a = inkTopAboveBaseline(text, family, weight, sizePx)        // actualBoundingBoxAscent
    return ceil( a if a>0 and exists else sizePx*0.75 )          // 兜底 0.75em

measure.descent(text, family, weight, sizePx):
    if text == "" return 0
    d = inkBottomBelowBaseline(...)                              // actualBoundingBoxDescent
    return ceil( d if exists else sizePx*0.2 )                   // 兜底 0.2em

measure.ink(text, family, weight, sizePx) -> {abl, abr, adv}:
    if text == "" return {0,0,0}
    // 对齐点在原点、文本从 x=0 起排时的墨区坐标：
    abl = actualBoundingBoxLeft     // 通常为负（左字肩向右缩进）；abl = -lsb
    abr = actualBoundingBoxRight    // 墨区右缘；abr = adv - rsb
    adv = measureText(text).width
    兜底（无 API 时）：abl = -sizePx*0.05, abr = sizePx*0.85
    return {abl, abr, adv}

// 关键恒等式（后面所有公式依赖它）：
//   墨区宽 = abr + abl        （abl 为负时才是正的墨宽！不是 abr - abl）
//   左字肩 lsb = -abl；右字肩 rsb = adv - abr
```

**Java 移植提醒**
- 必须保证「测量器自洽」：adv、abl、abr、ascent、descent 来自同一字体/同一 hinting 设置；不要把 `FontMetrics.stringWidth` 与 `GlyphVector` 的墨区混用两家实现。
- Web 端测量用 `ceil` 取整后再参与布局（元素宽度、ascent/descent）；`ink` 的 abl/abr/**不取整**。Java 端照抄取整点，否则宽度会有 1px 级连锁偏差。
- 单字测量和整串测量是两个接口，不要用单字之和冒充整串宽度（kerning 差异）——Web 源码是先例（`render.js:141-142` vs `render.js:253-274`）。

---

## 2. 内容盒（padding → 像素）

```
// render.js:46-52
contentBox(padding, h):
    t = padding.top    * h
    r = padding.right  * h
    b = padding.bottom * h
    l = padding.left   * h
    return {t, r, b, l, top: t, size: h - t - b}

// 用法
p = contentBox(el.props.padding, h)
s = p.size                 // 元素「内容尺寸」；所有字号/色条/图标尺寸都是 s 的函数
元素盒宽 = 各类型 width（见下）; 元素盒高 = h
内容区 = (x=p.l, y=p.t, w=s, h=s)   // 内容区是正方形（宽高都 = s），但元素宽度通常 > s
```

注意：`size` 可以为负（padding.top+bottom > 1 时不会被 clamp 到一起，反序列化各自 clamp 到 1，仍可能和 >1）；实际 UI 不会出现，但复刻不要额外改写。

---

## 3. 元素宽度总表（各类型 metrics.width）

```
// 通用：以下所有 width 都已含左右 padding；s = p.size
arrow        : s + p.l + p.r                                   // render.js:175
bilingual    : max(zhW, enW) + p.l + p.r                       // render.js:210-211
small-biling : max(zhW, enW) + p.l + p.r（字号按 0.75s 计算）    // render.js:184-186,188-213
big-number   : textW + p.l + p.r                               // render.js:230
number-line  : 左对齐: labelX + label.width + p.r              // render.js:382
               右对齐: stripesX0 + Σ stripeW + p.r
text-line    : sink 左: p.l + stripeW + gap + textW + gap + label.width + p.r
               sink 右: stripeX + stripeW + p.r   (stripeX = p.l+textW+gap+label.width+gap)
               flat 左: p.l + stripeW + gap + flat.width + p.r
               flat 右: stripeX + stripeW + p.r   (stripeX = p.l+flat.width+gap)
entrance     : p.l + codeW + label.width + p.r                  // render.js:479
exit 左      : p.l + codeW + label.width + p.r
exit 右      : p.l + label.width + codeW + p.r                  // render.js:470
space        : widthRatio * h     （不含 padding！）             // render.js:492
icon         : s + p.l + p.r                                    // render.js:499
```

---

## 4. 行内水平布局（三通道 / 动态单通道）

```
// render.js:542-580
elementAlign(el): return el.props.elementAlign if in {'center','right'} else 'left'

layoutRowElements(row, h, measure, signWidth, alignEnabled):
    slots = [ {id, width: computeElementWidth(el, h, measure)} for el in row.elements ]
    centerTotal = 0
    if alignEnabled:
        centerTotal = Σ slots[i].width where elementAlign(row.elements[i])=='center'
    leftX = 0
    rightX = signWidth
    centerX = (signWidth - centerTotal) / 2
    for i in 0..slots.length-1:
        a = (alignEnabled ? elementAlign(row.elements[i]) : 'left')
        if a == 'right':  rightX -= slots[i].width;  slots[i].x = rightX
        elif a == 'center': slots[i].x = centerX;    centerX += slots[i].width
        else:             slots[i].x = leftX;        leftX  += slots[i].width
    return slots
```

- `alignEnabled = (sign.widthMode === 'fixed')`（`render.js:587`）。
- 贴右通道：数组序第一项先排、排在最右；因此数组序与视觉序相反。
- 三通道之间可以重叠（不检查碰撞）。

```
// render.js:531-539 / 582-594
computeSignWidth(sign, measure):
    if sign.widthMode == 'fixed': return sign.width
    w = sign.rowHeight
    for row in sign.rows: w = max(w, Σ computeElementWidth(el, sign.rowHeight, measure))
    return round(w)

layoutSign(sign, measure):
    h = sign.rowHeight
    W = computeSignWidth(sign, measure)
    H = h * sign.rows.length
    rows = []
    for ri,row in sign.rows:
        elements = layoutRowElements(row, h, measure, W, sign.widthMode=='fixed')
        rows.push({id: row.id, y: ri*h, height: h, elements,
                   contentWidth: Σ elements.width})
    return {width: W, height: H, rows}
```

---

## 5. 灰框几何

```
// render.js:597-606
rowFrameRects(signWidth W, rowY, rowHeight h, fw, first, last):
    top    = (first ? fw : fw/2)
    bottom = (last  ? fw : fw/2)
    return [
      {x:0,        y:rowY,          width:fw, height:h},   // 左竖条
      {x:W - fw,   y:rowY,          width:fw, height:h},   // 右竖条
      {x:0,        y:rowY,          width:W,  height:top}, // 上横条
      {x:0,        y:rowY+h-bottom, width:W,  height:bottom} // 下横条
    ]
// 仅在 sign.frameWidth > 0 时画；画在该行元素之后（遮挡越界内容）
```

---

## 6. 箭头几何（`render.js:56-127`）

```
// 轴向（left/right/up/down）：箭盒 size×size
axisArrowGeometry(size, thickness, direction):
    c = size/2
    k = thickness / (2*sqrt(2))          // 箭柄半厚
    switch direction:
      'left':
        head1 = [[0,c],[thickness,c],[thickness+c,0],[c,0]]
        head2 = [[0,c],[thickness,c],[thickness+c,size],[c,size]]
        shaft = [[thickness/2,c-k],[size,c-k],[size,c+k],[thickness/2,c+k]]
      'right':
        head1 = [[size,c],[size-thickness,c],[size-thickness-c,0],[size-c,0]]
        head2 = [[size,c],[size-thickness,c],[size-thickness-c,size],[size-c,size]]
        shaft = [[0,c-k],[size-thickness/2,c-k],[size-thickness/2,c+k],[0,c+k]]
      'up':
        head1 = [[c,0],[c,thickness],[0,thickness+c],[0,c]]
        head2 = [[c,0],[c,thickness],[size,thickness+c],[size,c]]
        shaft = [[c-k,thickness/2],[c+k,thickness/2],[c+k,size],[c-k,size]]
      'down':
        head1 = [[c,size],[c,size-thickness],[0,size-thickness-c],[0,size-c]]
        head2 = [[c,size],[c,size-thickness],[size,size-thickness-c],[size,size-c]]
        shaft = [[c-k,0],[c+k,0],[c+k,size-thickness/2],[c-k,size-thickness/2]]
    return {head1, head2, shaft}

// 斜向：基底轴箭头在虚拟盒 L×L 内构建，再旋转 ±45°，最后把箭尖对齐内容盒角
DIAGONAL = {
  'left-up':    {base:'left',  phi:+45°, corner:[0,0]},
  'right-up':   {base:'right', phi:-45°, corner:[1,0]},
  'right-down': {base:'right', phi:+45°, corner:[1,1]},
  'left-down':  {base:'left',  phi:-45°, corner:[0,1]}
}

arrowGeometry(size, thickness, direction):
    diag = DIAGONAL[direction]
    if diag == null: return axisArrowGeometry(size, thickness, direction)
    L = size*sqrt(2) - thickness
    g = axisArrowGeometry(L, thickness, diag.base)
    phi = diag.phi (弧度)
    cosφ=cos(phi); sinφ=sin(phi); cl = L/2
    tip = (base=='left') ? [0,cl] : [L,cl]

    rel(p):                                    // 绕虚拟盒中心旋转
        dx = p[0]-cl; dy = p[1]-cl
        return [dx*cosφ - dy*sinφ, dx*sinφ + dy*cosφ]

    tipRot = rel(tip)
    ox = diag.corner[0]*size - tipRot[0]       // 平移使箭尖落在目标角
    oy = diag.corner[1]*size - tipRot[1]
    rot(p) = { q = rel(p); return [q[0]+ox, q[1]+oy] }

    return { head1: g.head1.map(rot), head2: g.head2.map(rot), shaft: g.shaft.map(rot) }

// arrowMetrics（render.js:170-176）
arrowMetrics(el, h, m):
    p = contentBox(el.props.padding, h)
    s = p.size
    th = clamp(el.props.thicknessRatio, 0.05, 0.95) * s
    geo = arrowGeometry(s, th, el.props.direction)
    return {pad:p, size:s, geo, width: s+p.l+p.r, offset:{x:p.l, y:p.t}}

// 绘制：三块多边形平移到 (p.l, p.t)，fill = color，无描边
```

---

## 7. 双语标签与双语文本（`render.js:136-213,635-668`）

```
// 组合元素内部复用：标签自身内容尺寸 boxSize，宿主内容尺寸 cs
bilingualMetrics(textZh, textEn, boxSize, m, bold, cs):
    weight   = bold ? 700 : 400
    content  = cs or boxSize
    zhSize   = boxSize * (506/800)      // 0.6325
    enSize   = boxSize * (285/800)      // 0.35625
    zhW      = m(textZh, FONT_ZH, weight, zhSize)
    enW      = m(textEn, FONT_EN, weight, enSize)
    zhInk    = m.ink(textZh, FONT_ZH, weight, zhSize)
    enInk    = m.ink(textEn, FONT_EN, weight, enSize)
    zhAscent = m.ascent(textZh, FONT_ZH, weight, zhSize)
    zhDescent= m.descent(textZh, FONT_ZH, weight, zhSize)
    enAscent = m.ascent(textEn, FONT_EN, weight, enSize)

    // 垂直墨区锚定（cs 为宿主内容尺寸！）
    zhBase = 0.036*content + zhAscent
    enBase = zhBase + zhDescent + 0.123*content + enAscent

    return {width: max(zhW,enW), zhSize, enSize, zhW, enW,
            zhBase, enBase, zhAbl:zhInk.abl, zhAbr:zhInk.abr,
            enAbl:enInk.abl, enAbr:enInk.abr, weight,
            zhAscent, zhDescent, enAscent}
```

```
// 独立双语文本（bilingual-text 用 boxRatio=1；small-bilingual-text 用 boxRatio=0.75）
standaloneBilingualMetrics(el, h, m, boxRatio):
    p = contentBox(el.props.padding, h)
    bi = bilingualMetrics(el.props.textZh, el.props.textEn,
                          p.size*boxRatio, m, el.props.bold, p.size)   // cs = p.size
    if boxRatio == 1:                                  // 只有大号做整组垂直居中
        tops = []; bottoms = []
        if trim(textZh) != "":
            tops.push(bi.zhBase - bi.zhAscent)
            bottoms.push(bi.zhBase + bi.zhDescent)
        if trim(textEn) != "":
            tops.push(bi.enBase - bi.enAscent)
            bottoms.push(bi.enBase + m.descent(textEn, FONT_EN, bi.weight, bi.enSize))
        if tops.length > 0:
            shift = (p.size - max(bottoms) - min(tops)) / 2
            bi.zhBase += shift; bi.enBase += shift
    bi.pad     = p
    bi.textW   = bi.width                // 纯文本区宽度（不含 padding）
    bi.width   = bi.width + p.l + p.r
    return bi
```

```
// 水平对齐（render.js:635-649 / 656-668）
bilingualAlignX(label, x, align, indent):
    if align == 'left':   return {zhX: x, enX: x + indent}
    if align == 'right':  return {zhX: x + label.width - label.zhW,
                                  enX: x + label.width - label.enW - indent}
    return {zhX: x + (label.width - label.zhW)/2,
            enX: x + (label.width - label.enW)/2}

drawBilingualBox(textZh, textEn, label, x, top, color, align, indentScale):
    indent = indentScale * (1/30)                  // 只有双语文本元素传 indentScale=s；组合标签传 0
    pos = bilingualAlignX(label, x, align, indent)
    drawText(textZh, x=pos.zhX, y=top+label.zhBase, family=FONT_ZH,
             size=label.zhSize, weight=label.weight, fill=color)
    drawText(textEn, x=pos.enX, y=top+label.enBase, family=FONT_EN,
             size=label.enSize, weight=label.weight, fill=color)
```

**做错点**：`drawBilingualText` 的 `x` 基准是 **`mt.textW`（不含 padding 的纯文本区）**，不是 `mt.width`；调用时传 `x = p.l`、`top = p.t`（`render.js:685-695`）。

---

## 8. 数字墨区排版（`render.js:234-321`）

### 8.1 `digitLayout(num, slotSize, fontSize, m, font)`

```
digitLayout(num, s, fontSize, m, font):
    font = font or FONT_NUM
    chars = split(num, '')                     // 注意：按 UTF-16 码元逐字符；数字/常规文本可视为逐字
    if chars.length == 0:
        return {digits:[], advance:s, margin:0}
    ref = m.ink('3', font, 400, fontSize)
    margin = (s - ref.adv)/2 - ref.abl         // = (s - adv3)/2 + lsb3   （基准墨边距 M₃）
    if chars.length == 1:
        ink = m.ink(chars[0], font, 400, fontSize)
        inkW = ink.abr + ink.abl               // 墨区宽（abl 为负）
        return {digits:[{ch:chars[0], x: margin + ink.adv/2 + ink.abl}],
                advance: margin*2 + inkW, margin}
    gap = (-ref.abl) + (ref.adv - ref.abr)     // lsb3 + rsb3
    inkLeft = margin
    digits = []
    for ch in chars:
        ink = m.ink(ch, font, 400, fontSize)
        x = inkLeft + ink.adv/2 + ink.abl      // text-anchor=middle 的绘制点
        digits.push({ch, x})
        inkLeft += (ink.abr + ink.abl) + gap   // 墨宽 + 固定墨区间距
    advance = inkLeft - gap + margin           // 末位墨区右缘 + M
    return {digits, advance, margin}
```

语义：首位墨区左缘 = `margin`；相邻数字墨区间距恒为 `gap`；末位墨区右缘 + `margin` = 总宽；单位数字两侧各留 `margin`（因此「1」的间距与「12」的首/末间距一致）。**「3」是所有间距的标定基准，必须用同一字体同一字号的 `m.ink('3')`。**

### 8.2 `inkTextLayout(text, slotSize, fontSize, m, font, edge)`

```
inkTextLayout(text, slotSize, fontSize, m, font, edge):
    if text == "":
        textW  = (edge=='margin') ? slotSize : 0
        return {chars:[], textW, margin:0,
                firstInkLeft: (edge=='margin'? slotSize:0),
                lastInkRight: (edge=='margin'? slotSize:0)}
    if text.length == 1 and edge != 'margin':
        w   = m(text, font, 400, fontSize)          // 单字整串宽
        ink = m.ink(text, font, 400, fontSize)
        return {chars:[{ch:text, x:w/2}], textW:w, margin:0,
                firstInkLeft:  w/2 - ink.adv/2 - ink.abl,
                lastInkRight:  w/2 - ink.adv/2 + ink.abr}
    layout = digitLayout(text, slotSize, fontSize, m, font)
    if edge == 'margin':
        return {chars:layout.digits, textW:layout.advance, margin:layout.margin,
                firstInkLeft: layout.margin,
                lastInkRight: layout.advance - layout.margin}
    // edge == 'lsb'：首位墨区锚定自身字距边距 lsb，整体左移，去掉 M₃ 的左边距
    firstInk = m.ink(text[0], font, 400, fontSize)
    shift = -firstInk.abl - layout.margin
    chars = [{ch:d.ch, x:d.x + shift} for d in layout.digits]
    lastInk = m.ink(text[last], font, 400, fontSize)
    textW = chars[last].x + lastInk.adv/2                  // 末位前进盒右缘
    return {chars, textW, margin: layout.margin,
            firstInkLeft:  chars[0].x - firstInk.adv/2 - firstInk.abl,
            lastInkRight:  chars[last].x - lastInk.adv/2 + lastInk.abr}
```

- `edge='margin'`：用于**数字线路的线路号**（数字组紧贴色块/标签，两侧留基准墨边距 M₃）。
- `edge='lsb'`：用于**独立文本**（大文本、文本线路大字、出入口/出口编号），盒边距不随内容变化。

### 8.3 `bigNumberMetrics`

```
// render.js:215-232
bigNumberMetrics(el, h, m):
    p = contentBox(el.props.padding, h); s = p.size
    fontSize = s * 1.3
    font = FONT_NUM
    text = String(el.props.text == null ? '' : el.props.text)
    base = p.t + 0.036*s + m.ascent(text, font, 400, fontSize)   // 墨区顶对齐
    t = inkTextLayout(text, s, fontSize, m, font, 'lsb')
    optical = (t.firstInkLeft - (t.textW - t.lastInkRight)) / 2  // 左右墨区间距差的一半
    digits = [{ch:d.ch, x:d.x - optical} for d in t.chars]       // 整体平移做光学居中
    return {pad:p, font, fontSize, textW:t.textW, digits, base,
            width: t.textW + p.l + p.r}

// 绘制：对每个 digit：
//   x = p.l + digit.x, y = base, text-anchor=middle,
//   font-family=FONT_NUM, font-size=fontSize, fill=color
```

**做错点**：宽度用的是**平移前**的 `textW`，不是平移后视觉外接宽度；文字绘制点在 `p.l + digit.x`。

---

## 9. 数字线路（`render.js:323-384,708-722`）

```
numberLineMetrics(el, h, m):
    p = contentBox(el.props.padding, h); s = p.size
    stripeW = s * (10/28)
    label = bilingualMetrics('号线', 'Line', s*0.75, m, false, s)
    labelTop = p.t + label.zhBase - label.zhAscent          // 「号线」墨区顶
    right = (el.props.align == 'right')
    lines = right ? (el.props.lines or []).slice(0,1) : (el.props.lines or [])
    layouts = []
    for line in lines:
        num = String(line.number or '')
        t = inkTextLayout(num, s, s*1.3, m, FONT_NUM, 'margin')
        layouts.push({ num, color: line.color, digits:t.chars,
                       advance:t.textW, margin:t.margin,
                       base: labelTop + m.ascent(num, FONT_NUM, 400, s*1.3) })
    stripesW = layouts.length * stripeW
    firstMargin = layouts.length ? layouts[0].margin : 0

    if right:
        labelX = p.l + Σ layouts.advance
        stripesX0 = labelX + label.width + firstMargin
        labelAlign = 'right'
    else:
        labelX = p.l + Σ layouts.advance + stripesW
        stripesX0 = p.l
        labelAlign = 'left'

    entries = []; advAcc = 0
    for i, L in layouts:
        e = {number:L.num, color:L.color, stripeW, advance:L.advance,
             font:FONT_NUM, fontSize:s*1.3, base:L.base, digits:L.digits}
        if right:
            e.digitsX = p.l + advAcc                    // 编号从内容左缘起排
            e.stripeX = stripesX0 + i*stripeW           // 色块组在标签右侧并排
        else:
            e.stripeX = p.l + i*stripeW + advAcc        // 每条：色条在前、编号在后
            e.digitsX = e.stripeX + stripeW
        advAcc += L.advance
        entries.push(e)

    width = right ? stripesX0 + stripesW + p.r
                  : labelX + label.width + p.r
    return {pad:p, size:s, stripeH: h - p.t, entries, label, labelAlign,
            labelX, stripeX:stripesX0, width}

// 绘制顺序：对每条 entry： rect(x=stripeX, y=p.t, w=stripeW, h=stripeH, fill=color)
//                    然后 digits 逐字 text(x=e.digitsX+digit.x, y=e.base,
//                    text-anchor=middle, family=FONT_NUM, size=fontSize, fill=textColor)
//            最后 drawBilingualBox('号线','Line', label, labelX, p.t, textColor, labelAlign, indentScale=0)
```

**做错点**
1. 色条高度 = `h − p.t`（一直画到行底），不是 `s`。
2. 数字基线用「号线」墨区顶 + 数字 ascent（数字墨顶与标签墨顶齐平），不是内容顶 + 0.036s 的独立公式（等价，但别双重加 padding）。
3. right 模式只渲染第 1 条线路；`labelX` 与 `stripesX0` 的先后关系是「数字 → 标签 → 色块」。
4. 左对齐时数组顺序是「色条0、数字0、色条1、数字1…」但 x 用 `advAcc` 累加，不是简单的固定步长。

---

## 10. 文本线路（`render.js:386-440,724-746`）

```
textLineMetrics(el, h, m):
    p = contentBox(el.props.padding, h); s = p.size
    stripeW = s*(10/28)
    gap = s/8
    right = (el.props.align == 'right')
    labelAlign = right ? 'right' : 'left'

    if el.props.nameSink != false:                        // 缺省视为 true
        fontSize = s
        textT = inkTextLayout(el.props.text, s, fontSize, m, FONT_ZH, 'lsb')
        label = bilingualMetrics('线', el.props.textEn, s*0.75, m, false, s)
        base = p.t + label.zhBase - label.zhAscent
                   + m.ascent(el.props.text, FONT_ZH, 400, fontSize)   // 大字墨顶 = 「线」墨顶
        if right:
            textX   = p.l
            labelX  = p.l + textT.textW + gap
            stripeX = labelX + label.width + gap
            width   = stripeX + stripeW + p.r
        else:
            textX   = p.l + stripeW + gap
            labelX  = p.l + stripeW + gap + textT.textW + gap
            stripeX = p.l
            width   = p.l + stripeW + gap + textT.textW + gap + label.width + p.r
        return {pad:p, size:s, sink:true, stripeW, stripeH:h-p.t, stripeX,
                fontSize, textW:textT.textW, base, textX,
                textChars:textT.chars, gap, label, labelX, labelAlign, width}

    // 平铺（线路名下沉关闭）
    flat = bilingualMetrics(el.props.text, el.props.textEn, s*0.75, m, false, s)
    if right:
        labelX  = p.l
        stripeX = labelX + flat.width + gap
        width   = stripeX + stripeW + p.r
    else:
        labelX  = p.l + stripeW + gap
        stripeX = p.l
        width   = p.l + stripeW + gap + flat.width + p.r
    return {pad:p, size:s, sink:false, stripeW, stripeH:h-p.t, stripeX,
            gap, label:flat, labelX, labelAlign, width}

// 绘制：先画色条 rect(stripeX, p.t, stripeW, stripeH, blockColor)
//       sink：大字逐字 text(textX + d.x, base, anchor=middle, family=FONT_ZH, size=s, fill=textColor)
//             + drawBilingualBox('线', textEn, label, labelX, p.t, textColor, labelAlign, 0)
//       flat：只画 drawBilingualBox(text, textEn, flat, labelX, p.t, textColor, labelAlign, 0)
```

注意：平铺模式**不显示「线」字后缀**，中文显示输入全文；下沉模式「线」是固定字，`text` 是色条右侧大字。

---

## 11. 出入口 / 出口（`render.js:442-489,748-774`）

```
codeLabelMetrics(el, h, m, labelZh, labelEn, alignable):
    p = contentBox(el.props.padding, h); s = p.size
    size = s * 1.3                              // 编号字号
    code = String(el.props.code == null ? '' : el.props.code)
    label = bilingualMetrics(labelZh, labelEn, s*0.75, m, false, s)
    right = alignable and (el.props.align == 'right')

    codeBase = p.t + label.zhBase - label.zhAscent
                   + m.ascent(code, FONT_NUM, 400, size)      // 编号墨顶 = 标签墨顶
    codeT = inkTextLayout(code, s, size, m, FONT_NUM, 'lsb')
    ink3  = m.ink('3', FONT_NUM, 400, size)
    trailingMargin = (s - ink3.adv)/2 - ink3.abl                 // 基准墨边距 M₃
    codeW = codeT.lastInkRight + trailingMargin                  // 编号宽（含末位 M₃）

    if right:                                    // 仅 exit 可能为 true
        rightCodeX = p.l + label.width + trailingMargin
        return {pad:p, size:s, codeSize:size, codeW, codeBase,
                codeCenterX: rightCodeX + codeW/2,
                codeChars: codeT.chars, codeX: rightCodeX,
                gap: trailingMargin, label, labelX: p.l, labelAlign:'right',
                width: p.l + label.width + codeW + p.r}
    return {pad:p, size:s, codeSize:size, codeW, codeBase,
            codeCenterX: p.l + codeW/2,
            codeChars: codeT.chars, codeX: p.l,
            gap: trailingMargin, label, labelX: p.l + codeW, labelAlign:'left',
            width: p.l + codeW + label.width + p.r}

// 绘制：若 codeChars 非空 → 逐字 text(codeX + d.x, codeBase, anchor=middle, family=FONT_NUM,
//                                   size=codeSize, fill=color)
//       否则（空 code）→ 整串 text(codeCenterX, codeBase, anchor=middle, …)
//       再 drawBilingualBox(labelZh, labelEn, label, labelX, p.t, color, labelAlign, 0)

entranceMetrics = codeLabelMetrics(el,h,m,'出入口','Entrance', false)
exitMetrics     = codeLabelMetrics(el,h,m,'出口','Exit',      true)
```

**做错点**
- `gap = s/8` 虽然在函数里计算，但**不参与编号↔标签间距**；间距就是 `trailingMargin`（已含于 codeW / 右对齐的标签侧留白）。
- 出口背景不是画在这里：由通用渲染层用 `backgroundColor = #F7D917` 画整元素盒。
- exit 右对齐时绘制调用顺序仍是「先编号后标签」，但坐标把编号放在标签右侧。

---

## 12. 空白占位与图标（`render.js:491-500,776-796`）

```
spaceMetrics(el, h, m): return { width: el.props.widthRatio * h }
// 宽度与 padding 无关；无内容。

iconMetrics(el, h, m):
    p = contentBox(el.props.padding, h); s = p.size
    return {pad:p, size:s, width: s + p.l + p.r}

drawIcon(el, mt):
    icon = SignIcons.get(el.props.icon)              // 未知 id 回退 elevator
    rotation = (icon.cat == 'service') ? Number(el.props.rotation) : 0
    if rotation not in {-90, 90, 180}: rotation = 0   // 0 不加 transform
    nested = <svg x=mt.pad.l y=mt.pad.t width=mt.size height=mt.size
                  viewBox=icon.vb preserveAspectRatio="xMidYMid meet">
    if rotation != 0:
        nested.transform = "rotate(" + rotation + " " +
                           (mt.pad.l + mt.size/2) + " " + (mt.pad.t + mt.size/2) + ")"
    nested.innerHTML = icon.body.replaceAll('fill="black"', 'fill="' + (props.color or '#000000') + '"')
```

---

## 13. 单元素渲染与整牌合成（`render.js:814-911`）

```
renderElement(el, rowHeight, measure, opts):
    g = <g class="element" [data-element-id=el.id if !clean]>
    mt = elementMetrics(el, rowHeight, measure)
    w = max(mt.width, 1)
    boxW = ceil(w)                                  // 背景/命中框取整，消除亚像素缝
    if el.props.backgroundColor != null:
        g.appendChild(<rect x=0 y=0 width=boxW height=rowHeight fill=bg>)
    drawFns[el.type](g, el, mt)                     // 局部坐标
    if !clean:
        g.appendChild(<rect class="hitbox" x=0 y=0 width=boxW height=rowHeight
                            fill="transparent" pointer-events="all">)
    return {node:g, width:w}

renderSignInto(svg, sign, measure, opts):
    layout = layoutSign(sign, measure)
    clear(svg)
    svg.viewBox = "0 0 " + layout.width + " " + layout.height
    svg.appendChild(<rect class="sign-bg" x=0 y=0 w=layout.width h=layout.height
                          fill=sign.backgroundColor>)
    for ri, row in sign.rows:
        rowLayout = layout.rows[ri]
        rowG = <g class="sign-row" [data-row-id]>
        rowG.appendChild(<rect class="row-bg" x=0 y=rowLayout.y w=layout.width
                               h=sign.rowHeight
                               fill=(row.backgroundColor or sign.backgroundColor)>)
        for ei, el in row.elements:
            rendered = renderElement(el, sign.rowHeight, measure, {clean: opts.clean})
            rendered.node.transform =
                "translate(" + floor(rowLayout.elements[ei].x) + "," + rowLayout.y + ")"
            if selected: class += " selected"
            rowG.appendChild(rendered.node)
        if sign.frameWidth > 0:
            appendFrameRects(rowG,
                rowFrameRects(layout.width, rowLayout.y, sign.rowHeight,
                              sign.frameWidth, ri==0, ri==rows.length-1),
                sign.frameColor or '#808080')
        svg.appendChild(rowG)
    return layout
```

**顺序要点**：sign-bg → 逐行 [row-bg → elements → frame]；frame 叠加在元素之上；行与行之间 frame 边界各画半宽。

---

## 14. 相邻自动内边距（`state.js:532-577`）

```
elementLane(el, fixed):
    if !fixed: return 'left'
    a = el.props.elementAlign
    return (a=='right') ? 'right' : (a=='center') ? 'center' : 'left'

applyPaddingAuto(sign):                    // 幂等；每次 update 后执行
    fixed = (sign.widthMode == 'fixed')
    for row in sign.rows:
        els = row.elements
        for i, el in els:
            if el.type == 'space' or !el.props.paddingAuto: continue
            lane = elementLane(el, fixed)
            neighborAt(delta):
                j = i + delta
                if j < 0 or j >= els.length: return null
                n = els[j]
                if n.type == 'space': return null
                return (elementLane(n, fixed) == lane) ? n : null
            leftNeighbor  = neighborAt(-1)
            rightNeighbor = neighborAt(+1)
            // 贴右通道数组序与视觉序相反
            hasLeft  = (lane == 'right') ? (rightNeighbor != null) : (leftNeighbor != null)
            hasRight = (lane == 'right') ? (leftNeighbor  != null) : (rightNeighbor != null)
            pl = hasLeft  ? 0.1 : 0.2        // PADDING_AUTO_ADJACENT / DEFAULT
            pr = hasRight ? 0.1 : 0.2
            if el.props.padding.left != pl or el.props.padding.right != pr:
                set el.props.padding.left = pl; el.props.padding.right = pr
    return sign
```

**做错点**：邻居只看数组 `i±1`（不是「同通道最近元素」）；`space` 既不算邻居也不参与自动；手动改左/右会把 `paddingAuto=false` 钉住（写入路径 `state.js:307-320`）。

---

## 15. 拖放落点 `dropTargetAt`（`interact.js:412-462`）

```
dropTargetAt(rowIndex, x, align, draggedWidth):
    slots = layout.rows[rowIndex].elements       // [{x,width}]
    row   = state.rows[rowIndex]
    fixed = (state.widthMode == 'fixed')
    lane = (fixed and align=='right') ? 'right'
         : (fixed and align=='center') ? 'center' : 'left'
    laneIdx = [i for i,el in row.elements
                 where (fixed ? elementLane(el,true) : 'left') == lane]

    if laneIdx is empty:
        if lane == 'center':
            return {gap: slots.length,
                    indicatorX: max(0, (layout.width - draggedWidth)/2)}
        return {gap: slots.length,
                indicatorX: (lane=='right') ? layout.width : 0}

    j = 0; gap; indicatorX
    if lane == 'right':
        // 视觉右→左遍历
        while j < laneIdx.length and slots[laneIdx[j]].x + slots[laneIdx[j]].width/2 > x: j++
        gap = (j==0) ? laneIdx[0] : laneIdx[j-1] + 1
        if j == 0:
            indicatorX = slots[laneIdx[0]].x + slots[laneIdx[0]].width
        elif j < laneIdx.length:
            indicatorX = slots[laneIdx[j]].x + slots[laneIdx[j]].width
        else:
            indicatorX = slots[laneIdx[last]].x
    else:
        // 视觉左→右遍历
        while j < laneIdx.length and slots[laneIdx[j]].x + slots[laneIdx[j]].width/2 < x: j++
        gap = (j < laneIdx.length) ? laneIdx[j] : laneIdx[last] + 1
        indicatorX = (j < laneIdx.length) ? slots[laneIdx[j]].x
                     : slots[laneIdx[last]].x + slots[laneIdx[last]].width
    return {gap, indicatorX}
```

- `gap` 是**当前布局（含被拖元素）中的数组间隙序号**，语义为「目标行内、位于第 gap 个元素之前」。
- `draggedWidth` 只在目标通道为空且 lane=center 时用于定位指示线；由调用方统计被拖元素中与 `dragAlign` 同通道的宽度和（dynamic 模式全部计入，`interact.js:644-653`）。

---

## 16. 多元素移动 `moveElements`（`state.js:368-383`）

```
moveElements(sign, ids, toRowId, gapIndex):
    target = sign.rows.find(row.id == toRowId)
    if !target or !isInteger(gapIndex): return sign
    selected = Set(ids)
    moved = []
    for row in sign.rows:
        for el in row.elements:
            if el.id in selected: moved.push(el)          // 行顺序 + 数组顺序
    if moved empty: return sign
    gap = clamp(gapIndex, 0, target.elements.length)      // 目标行当前元素数（含被拖元素）
    removedBefore = count(el in target.elements[0..gap-1] where el.id in selected)
    newRows = []
    for row in sign.rows:
        elements = [el for el in row.elements if el.id not in selected]
        if row.id == toRowId:
            insertAt = gap - removedBefore
            elements.splice(insertAt, 0, ...moved)
        newRows.push({...row, elements})
    return {...sign, rows:newRows}

// 例：行 [A,B,C]，拖 B 到 gap=2 → 移除后 [A,C]，insertAt = 2-1 = 1 → [A,B,C]（原位）
// 例：行 [A,B,C]，拖 A 到 gap=2 → 移除后 [B,C]，insertAt = 2-1 = 1 → [B,A,C]
```

`moveElementsBy(sign, ids, direction)`（左右微调 `<`/`>`，`state.js:342-366`）：
```
for row in sign.rows:
    for lane in ['left','center','right']:
        indices = [i for i,el in row.elements where elementLane(el, fixed)==lane]
        if indices.length < 2: continue
        if lane == 'right': indices.reverse()          // 视觉序
        start = (direction < 0) ? 1 : indices.length-2
        stop  = (direction < 0) ? indices.length : -1
        for j = start; j != stop; j -= direction:
            a = indices[j]; b = indices[j+direction]
            if a selected and b not selected: swap(elements[a], elements[b])
```

---

## 17. 画布显示尺寸 / 缩放（`ui.js:118-135`、`interact.js:1006-1030`）

```
fitSignDisplay():
    avail = min(canvasArea.clientWidth, 1600)
    displayW = min(avail, layout.width) * (App.canvasZoom or 1)     // 永不放大
    svg.style.width = displayW + "px"
    canvasWrap.style.width = svg.style.width
    overlay.style.width  = svg.style.width
    overlay.style.height = svg.getBoundingClientRect().height + "px"

applyCanvasZoom(zoom, anchorX, anchorY):        // 触屏 pinch 用
    zoom = clamp(zoom, 0.5, 4)
    rect = svg.getBoundingClientRect()
    ax = (anchorX - rect.left) / rect.width
    ay = (anchorY - rect.top ) / rect.height
    App.canvasZoom = zoom
    fitSignDisplay()
    rect2 = svg.getBoundingClientRect()
    scrollLeft += (rect2.left + ax*rect2.width ) - anchorX
    scrollTop  += (rect2.top  + ay*rect2.height) - anchorY

setCanvasZoom(zoom): App.canvasZoom = clamp(zoom,0.5,4); fitSignDisplay(); updateZoomBadge()
updateZoomBadge(): badge.textContent = round(App.canvasZoom*100) + "%"
```

（桌面无滚轮缩放；徽标点击 `setCanvasZoom(1)`；移动端双击空白 `<300ms`、位移 `<30px` 复位。）

---

## 18. 内边距拖拽档位（`ui.js:349-407`）

```
bindPaddingScrub(trigger, side, getElementId):
    on pointerdown(主键, 非disabled, 元素存在且被选中):
        breakHistoryGroup()
        session = {id, ids:selectedIds.join(','), pointerId, startX, lastX, moved:false}
        setPointerCapture
    on pointermove(同 pointerId):
        if 会话失效（元素变了/选择变了）: end()
        if |clientX - startX| >= 5: moved = true; 显示 scrubbing 视觉
        steps = trunc((clientX - lastX) / 8)          // 每 8 屏幕 px 一档
        if steps != 0:
            lastX += steps*8
            adjustPadding(side, steps*0.05, ids + ':padding-scrub:' + side)
    on pointerup/cancel/blur:
        清理监听/捕获/视觉; breakHistoryGroup(); 若 moved 则吞掉随后 click
```

`adjustPadding(side, delta, key)` 对每个选中元素执行 `adjustElementPadding`（`state.js:294-302`）：
```
value = round(clamp(padding[side] + delta, 0, 8) * 10000) / 10000
updateElementProps(sign, id, { padding: { [side]: value } })
```
`updateElementProps` 对 padding.left/right 的写入会设置 `paddingAuto=false`（`state.js:307-320`）。因此**拖动内边距与手动输入一样会钉住自动内边距**（下一次 `applyPaddingAuto` 会跳过它）。

---

## 19. 默认参数下的示例数值（h=192、padding=0.2）

| 量 | 值 |
|---|---|
| `p.t = p.b = p.l = p.r` | 38.4 |
| `s`（内容尺寸） | 115.2 |
| 内容区高 / 色条高 `h − p.t` | 153.6 |
| bilingual 中文字号 `s×0.6325` | 72.864 |
| bilingual 英文字号 `s×0.35625` | 41.04 |
| small-bilingual 中文字号 `0.75s×0.6325` | 54.648 |
| small-bilingual 英文字号 `0.75s×0.35625` | 30.78 |
| 大文本 / 编号字号 `s×1.3` | 149.76 |
| 数字线路色条宽 `s×10/28` | 41.142857… |
| 文本线路 gap `s/8` | 14.4 |
| 箭头默认厚度 `0.25×s` | 28.8 |
| 箭头元素宽（默认 padding） | 192 |
| 图标元素宽（默认 padding） | 192 |
| `space` 默认宽 `0.5×192` | 96 |
| 默认牌面 | 2048×192（8 格 × 0.75 格） |
| 墨区顶引导 `0.036×s` | 4.1472 |

> 这些数值可直接用作 Java 端布局单元测试的期望值（文本宽度相关值除外，依赖具体字体测量）。

---

## 20. 常见实现错误清单（源自源码注释与实际公式）

1. **把 padding 当像素**：padding 是相对行高的比例；`contentBox` 乘 `h` 才是像素（`state.js:11`）。
2. **把 `s` 当行高**：字号 = `s × 比例`，不是 `h × 比例`。
3. **数字间距用等宽字距**：必须用 `m.ink('3')` 标定的 `margin/gap`，否则「1」与两位数间距不一致（`render.js:234-274`）。
4. **右对齐通道按数组序取视觉序**：贴右通道数组第一项 = 最右，落点和指示线都要反转（`interact.js:412-462`）。
5. **宽度取整位置**：元素宽度 `w` 保留浮点参与布局，平移用 `floor(x)`、背景/命中框用 `ceil(w)`；提前 round 会累积错位（`render.js:820-825,879`）。
6. **灰框每行画整宽**：相邻行共享边界应各画一半（`render.js:597-606`）。
7. **数字线路色条高度**：`h − p.t`（到行底），不是 `s`（`render.js:378`）。
8. **标签字号用错 boxSize**：组合标签字号 = `宿主 s × 0.75 × 字体比例`，而垂直锚定用宿主 `s`（`render.js:136-166`）。
9. **垂直居中只对大号双语文本**：small-bilingual 不居中（`render.js:193-208`）。
10. **出口背景**：常量 `#F7D917`、不可透明、不可改；反序列化强制回填（`state.js:614`）。
11. **`exit` 与 `entrance` 的差别**：只有 exit 支持右对齐、标签文字不同、背景固定（`render.js:483-489`）。
12. **自动内边距的邻居判定**：只看数组相邻且同通道、跳过 space；贴右通道左右互换；手动改边距要钉住（§14）。
13. **数字线路右对齐只渲染第 1 条**（`render.js:329-331`）。
14. **动态宽度忽略 elementAlign**，且宽度下限是 `rowHeight`（`render.js:531-539`）。
15. **画布显示「不放大」**：`min(avail, layout.width)`（`ui.js:125`）。
16. **撤销合并**：同 key 800ms 内合并；连续拖动内边距用 continuous；文本输入 blur 才断组（`ui.js:66-94`、`panel.js:572-577`）。
17. **`arrow` 元素没有左栏入口**：方向卡片创建的是 `icon` 元素（`panel.js:40-53`）。
18. **图标旋转仅服务设施**（`render.js:783-785`）。

---

## 21. 建议的单元测试用例（可直接对照 Web 控制台）

| 用例 | 输入 | 期望 |
|---|---|---|
| contentBox | h=192, padding 0.2×4 | t=r=b=l=38.4, size=115.2 |
| layoutRow 三通道 | W=1000；元素宽 100(left), 200(center), 300(right) | x = 0, 400, 700 |
| layoutRow 右序 | 同通道两个 right 元素宽 100、200（数组序 [A,B]） | A.x=900（数组第一项贴右最外），B.x=700 |
| computeSignWidth dynamic | 两行内容宽 500、1200，rowHeight=192 | 1200 |
| rowFrameRects | W=1000,h=100,fw=8, 中间行 y=100 | 左 8px 竖条、右 (992,100,8,100)、上 (0,100,1000,4)、下 (0,196,1000,4) |
| paddingAuto | 同行同通道 A,B 都 auto | A.right=0.1, B.left=0.1；A.left=0.2, B.right=0.2 |
| paddingAuto 贴右 | 两个 right 元素数组序 [A,B]，B 在视觉左 | A.left=0.1, B.left=0.2, B.right=0.1, A.right=0.2（按视觉外/内边） |
| moveElements 同行 | [A,B,C]，拖 B 到 gap=2 | 顺序不变 |
| moveElements 同行 | [A,B,C]，拖 A 到 gap=2 | [B,A,C] |
| moveElements 跨行 | 行1 [A]，行2 [B,C]，A 拖到行2 gap=1 | 行1 []，行2 [B,A,C] |
| digitLayout 单字 | 合成度量：s=140, ref'3'{abl:-6,abr:92,adv:100}, '1'{abl:-20,abr:52,adv:60} | margin=26；x=36；advance=84 |
| arrow left | s=100, th=20 | c=50, k=7.0711；head1=[[0,50],[20,50],[70,0],[50,0]]；shaft=[[10,42.9289],[100,42.9289],[100,57.0711],[10,57.0711]] |
| textLine sink 宽度 | s=100、padding=0、合成 textW=40、label.width=50 | stripeW=35.7143、gap=12.5、width=150.7143（= p.l+stripeW+gap+textW+gap+label.width+p.r） |
| exit 右对齐宽度 | codeW=80, label.width=120, p.l=p.r=10 | width = 10+120+80+10 = 220 |
| frame 渲染顺序 | 任意 | 最后一层是灰框（覆盖元素） |

> 备注：涉及文本宽度的用例需先固定 `measure`（Web 端可用 `crossorigin` 的离线字体与固定浏览器版本；Java 端用同一度量器生成期望值）。

---

## 22. Java 端移植对照表（公式 → 实现要点）

| 公式/行为 | Java 实现建议 |
|---|---|
| `measure`（整串前进宽） | `GlyphVector.getGlyphPosition(n)`/`getLogicalBounds` 的 advance 累加；开启 fractional metrics；最后 `ceil` |
| `measure.ink` | `glyphVector.getGlyphOutline(i).getBounds2D()` 相对基线原点；`abl=left`、`abr=right`；注意与 Web 一样对整串测量（对多字符串取整体墨区） |
| `measure.ascent/descent` | 墨区上/下相对基线；`ceil`；无 API 时按 0.75em/0.2em 兜底 |
| 浮点坐标 | 用 `double/float` 保持设计像素浮点；只在绘制阶段映射到屏幕 |
| `floor/ceil/round` | Java `Math.floor/ceil/round` 与 JS 行为对正数一致（注意 JS `Math.round(-0.5)= -0`，本规格坐标基本非负） |
| 逐字拆字符串 | JS 按 UTF-16 码元；Java 用 `codePointAt` 逐码点更安全（BMP 汉字一致；emoji 会不同——源码未处理代理对，属边界差异） |
| 百分比/尺寸显示 | `Math.round(zoom*100)+"%"` 等文案照抄 |
| 默认值表 | 直接按 §4.4 建 Java 常量；反序列化清洗照 §4.9.2 |
