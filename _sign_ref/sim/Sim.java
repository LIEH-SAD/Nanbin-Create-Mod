import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;

import javax.imageio.ImageIO;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Offline simulator for Nanbin JS signs.
 * - executes the real script with Nashorn (--language=es6) and stub Text/Rect/Texture/Line builders
 * - rasterises the collected draw commands with the same font metric/layout rules as
 *   RenderCRTRailwaySign.renderCustomTextCRT + FittedTextRenderer
 *
 * Usage:
 *   java Sim --script <js> --out <png> --scenario train1|train2|rich|empty|loop3
 *            [--cols 11] [--res 0.75] [--px 2048]
 */
public final class Sim {

    static final float DESIGN_H_PX = 192f;
    static double RES_SCALE = 0.75;

    // ---------- font setup ----------
    static final String ROOT = "D:/NCM+mtr4.0/Nanbin/fabric/src/main/resources";
    static Font FONT_ALIBABA;
    static Font FONT_SOURCE_HAN;

    static void loadFonts() throws Exception {
        FONT_ALIBABA = Font.createFont(Font.TRUETYPE_FONT, new File(ROOT + "/assets/nanbin/font/alibabapuhuiti_regular.ttf"));
        FONT_SOURCE_HAN = Font.createFont(Font.TRUETYPE_FONT, new File(ROOT + "/assets/nanbin/font/sourcehansanssc_bold.otf"));
    }

    // ---------- draw command model ----------
    interface Cmd {
        void paint(Graphics2D g, double pxPerWorld);
    }

    static final class TextCmd implements Cmd {
        double x, y, w, h;
        String text = "";
        int color = 0xFF000000;
        double scale = 1.0;
        boolean bold, centered = true;

        public void paint(Graphics2D g, double pxPerWorld) {
            renderTextLikeJava(g, text, x, y, w, h, color, scale, bold, centered, pxPerWorld);
        }
    }

    static final class RectCmd implements Cmd {
        double x, y, w, h;
        int color = 0xFFFFFFFF;

        public void paint(Graphics2D g, double pxPerWorld) {
            g.setColor(new Color(color, true));
            g.fillRect((int) Math.round(x * pxPerWorld), (int) Math.round(y * pxPerWorld),
                    (int) Math.round(w * pxPerWorld), (int) Math.round(h * pxPerWorld));
        }
    }

    static final class TexCmd implements Cmd {
        double x, y, w, h;
        String tex = "";
        int color = 0xFFFFFFFF;

        public void paint(Graphics2D g, double pxPerWorld) {
            BufferedImage img = loadTex(tex);
            if (img == null) return;
            g.drawImage(img, (int) Math.round(x * pxPerWorld), (int) Math.round(y * pxPerWorld),
                    (int) Math.round(w * pxPerWorld), (int) Math.round(h * pxPerWorld), null);
        }
    }

    static final Map<String, BufferedImage> TEX_CACHE = new LinkedHashMap<>();

    static BufferedImage loadTex(String id) {
        if (TEX_CACHE.containsKey(id)) return TEX_CACHE.get(id);
        BufferedImage img = null;
        try {
            int i = id.indexOf(':');
            String path = i >= 0 ? ROOT + "/assets/" + id.substring(0, i) + "/" + id.substring(i + 1) : ROOT + "/" + id;
            if (!path.endsWith(".png")) path += ".png";
            img = ImageIO.read(new File(path));
        } catch (Exception e) {
            System.err.println("!! texture not found: " + id + " -> " + e.getMessage());
        }
        TEX_CACHE.put(id, img);
        return img;
    }

    // ---------- JS-facing globals ----------
    public static final class TextB {
        final TextCmd c = new TextCmd();

        public TextB pos(double x, double y) { c.x = x; c.y = y; return this; }
        public TextB size(double w, double h) { c.w = w; c.h = h; return this; }
        public TextB text(String t) { c.text = t == null ? "" : t; return this; }
        public TextB color(double col) { c.color = (int) (long) col; c.color = (c.color & 0xFF000000) == 0 ? c.color | 0xFF000000 : c.color; return this; }
        public TextB scale(double s) { c.scale = s; return this; }
        public TextB bold() { c.bold = true; return this; }
        public TextB bold(boolean b) { c.bold = b; return this; }
        public TextB centered() { c.centered = true; return this; }
        public TextB centered(boolean b) { c.centered = b; return this; }
        public void draw(SimCtx ctx) { ctx.cmds.add(c); }
    }

    public static final class TextFactory {
        public TextB create() { return new TextB(); }

        public double measure(String text, double height) { return measureWidth(text, (float) height); }
    }

    public static final class RectB {
        final RectCmd c = new RectCmd();

        public RectB pos(double x, double y) { c.x = x; c.y = y; return this; }
        public RectB size(double w, double h) { c.w = w; c.h = h; return this; }
        public RectB color(double col) { c.color = (int) (long) col; return this; }
        public void draw(SimCtx ctx) { ctx.cmds.add(c); }
    }

    public static final class RectFactory {
        public RectB create() { return new RectB(); }
    }

    public static final class TexB {
        final TexCmd c = new TexCmd();

        public TexB pos(double x, double y) { c.x = x; c.y = y; return this; }
        public TexB size(double w, double h) { c.w = w; c.h = h; return this; }
        public TexB texture(String t) { c.tex = t; return this; }
        public TexB color(double col) { c.color = (int) (long) col; return this; }
        public void draw(SimCtx ctx) { ctx.cmds.add(c); }
    }

    public static final class TexFactory {
        public TexB create() { return new TexB(); }
    }

    public static final class LineFactory {
        public LineB create() { return new LineB(); }
    }

    public static final class LineB {
        public LineB from(double x, double y) { return this; }
        public LineB to(double x, double y) { return this; }
        public LineB width(double w) { return this; }
        public LineB color(double c) { return this; }
        public void draw(SimCtx ctx) { }
    }

    public static final class SimCtx {
        final List<Cmd> cmds = new ArrayList<>();
        final double w, h;

        SimCtx(double w, double h) { this.w = w; this.h = h; }

        public double getWidth() { return w; }
        public double getHeight() { return h; }
        public double getCellSize() { return h; }
        public int getCellCount() { return (int) Math.round(w / h); }
    }

    // ---------- fake sign data ----------
    public static final class SimApi {
        double w, h;
        String[] stationNames = new String[0];
        long[] colors = new long[0];
        Map<Long, String> routeNames = new LinkedHashMap<>();
        Map<Long, String> routeNamesEn = new LinkedHashMap<>();
        Map<Long, String> routeNumbers = new LinkedHashMap<>();
        String[] exits = new String[0];
        boolean[] loops = new boolean[0];
        long[] stationIds = new long[0];

        public double getWidth() { return w; }
        public double getHeight() { return h; }
        public double getCellSize() { return h; }
        public int getCellCount() { return (int) Math.round(w / h); }
        public String getScreenType() { return "custom"; }
        public String getScreenKey() { return "sim"; }
        public int getSelectedStationIds() { return stationIds.length; }
        public long[] getSelectedStationIdList() { return stationIds; }
        public String getStationNames() {
            StringBuilder sb = new StringBuilder();
            for (String s : stationNames) { if (s != null && !s.isEmpty()) { if (sb.length() > 0) sb.append("/"); sb.append(s); } }
            return sb.toString();
        }
        public String getStationName(long id) { return stationNames.length > 0 ? stationNames[0] : ""; }
        public long[] getSelectedColors() { return colors; }
        public int getRouteColor(int i) { return i >= 0 && i < colors.length ? (int) colors[i] : 0; }
        public String getRouteName(long c) { String s = routeNames.get(c); return s == null ? "" : s; }
        public String getRouteName(long c, boolean en) {
            String s = en ? routeNamesEn.get(c) : routeNames.get(c);
            return s == null ? "" : s;
        }
        public String getRouteNumber(long c) { String s = routeNumbers.get(c); return s == null ? "" : s; }
        public String getRouteDestination(long c) { return ""; }
        public boolean isLoopRoute(long c) {
            for (int i = 0; i < colors.length; i++) if (colors[i] == c) return loops.length > i && loops[i];
            return false;
        }
        public String[] getRouteNumbers() {
            final java.util.List<String> list = new java.util.ArrayList<>();
            for (final long c : colors) {
                final String s = routeNumbers.get(c);
                if (s != null && !s.isEmpty()) {
                    list.add(s);
                }
            }
            return list.toArray(new String[0]);
        }
        public int getPlatformCount() { return 0; }
        public String[] getPlatformNumbers() { return new String[0]; }
        public int getExitCount() { return exits.length; }
        public String[] getExitNumbers() { return exits; }
        public String[] getExitDestinations(int i) { return new String[0]; }
        public String getExitInfo(int i) { return i >= 0 && i < exits.length ? exits[i] : ""; }
        public String getCustomText() { return ""; }
        public long getWorldTime() { return System.currentTimeMillis(); }
        public String getFormattedTime(String f) { return ""; }
        public int getBackgroundColor() { return 0; }
        public String[] getSignIds() { return new String[0]; }
        public String getCellSignId(int i) { return ""; }
        public int getCellIndex() { return 0; }
    }

    static SimApi buildApi(String scenario, double w, double h) {
        SimApi a = new SimApi();
        a.w = w;
        a.h = h;
        if ("train1".equals(scenario)) {
            a.stationNames = new String[]{"地铁站||Station"};
            a.colors = new long[]{0xFFFCD600L, 0xFF461D84L};
            a.routeNames.put(0xFFFCD600L, "3号线");
            a.routeNamesEn.put(0xFFFCD600L, "Line 3");
            a.routeNumbers.put(0xFFFCD600L, "3");
            a.routeNames.put(0xFF461D84L, "5号线");
            a.routeNamesEn.put(0xFF461D84L, "Line 5");
            a.routeNumbers.put(0xFF461D84L, "5");
            a.exits = new String[]{"1"};
            a.loops = new boolean[]{false, false};
            a.stationIds = new long[]{1234567890123L};
        } else if ("train2".equals(scenario)) {
            a.stationNames = new String[]{"地铁站||Station"};
            a.colors = new long[]{0xFFF2A900L};
            a.routeNames.put(0xFFF2A900L, "环线");
            a.routeNamesEn.put(0xFFF2A900L, "Loop Line");
            a.routeNumbers.put(0xFFF2A900L, "");
            a.exits = new String[]{"9A"};
            a.loops = new boolean[]{true};
            a.stationIds = new long[]{1234567890123L};
        } else if ("rich".equals(scenario)) {
            a.stationNames = new String[]{"小什字||Xiaoshizi"};
            a.colors = new long[]{0xFFFCD600L, 0xFF461D84L, 0xFF00A9A9L};
            a.routeNames.put(0xFFFCD600L, "3号线");
            a.routeNamesEn.put(0xFFFCD600L, "Line 3");
            a.routeNumbers.put(0xFFFCD600L, "3");
            a.routeNames.put(0xFF461D84L, "5号线");
            a.routeNamesEn.put(0xFF461D84L, "Line 5");
            a.routeNumbers.put(0xFF461D84L, "5");
            a.routeNames.put(0xFF00A9A9L, "环线");
            a.routeNamesEn.put(0xFF00A9A9L, "Loop Line");
            a.routeNumbers.put(0xFF00A9A9L, "");
            a.exits = new String[]{"2B"};
            a.loops = new boolean[]{false, false, true};
            a.stationIds = new long[]{1234567890123L};
        } else if ("empty".equals(scenario)) {
            // nothing selected
        } else if ("garbage".equals(scenario)) {
            // simulate getExitNumbers() decoding non-exit ids (station snowflake / route color) as garbage
            a.stationNames = new String[]{"\u5730\u94c1\u7ad9||Station"};
            a.colors = new long[]{0xFFFCD600L};
            a.routeNames.put(0xFFFCD600L, "3\u53f7\u7ebf");
            a.routeNamesEn.put(0xFFFCD600L, "Line 3");
            a.routeNumbers.put(0xFFFCD600L, "3");
            a.exits = new String[]{"\u00cb\u0004\u00fbq\u001f", "1"};
        } else if ("lianglukou".equals(scenario)) {
            a.stationNames = new String[]{"\u4e24\u8def\u53e3||Lianglukou"};
            a.colors = new long[]{0xFFEE3C3EL, 0xFF0066B1L};
            a.routeNames.put(0xFFEE3C3EL, "1\u53f7\u7ebf|Line 1");
            a.routeNamesEn.put(0xFFEE3C3EL, "1\u53f7\u7ebf|Line 1");
            a.routeNumbers.put(0xFFEE3C3EL, "1");
            a.routeNames.put(0xFF0066B1L, "3\u53f7\u7ebf|Line 3");
            a.routeNamesEn.put(0xFF0066B1L, "3\u53f7\u7ebf|Line 3");
            a.routeNumbers.put(0xFF0066B1L, "3");
            a.exits = new String[]{"6"};
            a.loops = new boolean[]{false, false};
        } else if ("jiangtiao".equals(scenario)) {
            a.stationNames = new String[]{"\u53cc\u798f||Shuangfu"};
            a.colors = new long[]{0xFF00A9A9L};
            a.routeNames.put(0xFF00A9A9L, "\u6c5f\u8df3\u7ebf|Jiangtiao Line");
            a.routeNamesEn.put(0xFF00A9A9L, "\u6c5f\u8df3\u7ebf|Jiangtiao Line");
            a.routeNumbers.put(0xFF00A9A9L, "");
            a.exits = new String[]{"1"};
        } else if ("nanwu".equals(scenario)) {
            a.stationNames = new String[]{"\u4e94\u91cc\u5e97||Wulidian"};
            a.colors = new long[]{0xFFE4007FL};
            a.routeNames.put(0xFFE4007FL, "\u5357\u4e94\u53f7\u7ebf|South Line 5");
            a.routeNamesEn.put(0xFFE4007FL, "\u5357\u4e94\u53f7\u7ebf|South Line 5");
            a.routeNumbers.put(0xFFE4007FL, "");
            a.exits = new String[]{"2"};
        } else if ("branch".equals(scenario)) {
            a.stationNames = new String[]{"\u4e24\u8def\u53e3||Lianglukou"};
            a.colors = new long[]{0xFFEE3C3EL};
            a.routeNames.put(0xFFEE3C3EL, "\u901a\u52e41\u53f7\u7ebf\u652f\u7ebf|Tong Qing 1branch line");
            a.routeNamesEn.put(0xFFEE3C3EL, "\u901a\u52e41\u53f7\u7ebf\u652f\u7ebf|Tong Qing 1branch line");
            a.routeNumbers.put(0xFFEE3C3EL, "1");
            a.exits = new String[]{"3"};
        } else if ("mixed".equals(scenario)) {
            a.stationNames = new String[]{"\u4e24\u8def\u53e3||Lianglukou"};
            a.colors = new long[]{0xFFEE3C3EL, 0xFF00A9A9L};
            a.routeNames.put(0xFFEE3C3EL, "3\u53f7\u7ebf|Line 3");
            a.routeNamesEn.put(0xFFEE3C3EL, "3\u53f7\u7ebf|Line 3");
            a.routeNumbers.put(0xFFEE3C3EL, "3");
            a.routeNames.put(0xFF00A9A9L, "\u6c5f\u8df3\u7ebf|Jiangtiao Line");
            a.routeNamesEn.put(0xFF00A9A9L, "\u6c5f\u8df3\u7ebf|Jiangtiao Line");
            a.routeNumbers.put(0xFF00A9A9L, "");
            a.exits = new String[]{"6"};
            a.loops = new boolean[]{false, false};
        } else if ("xian8".equals(scenario)) {
            a.stationNames = new String[]{"\u91d1\u7530||Jintian"};
            a.colors = new long[]{0xFF73BF70L};
            a.routeNames.put(0xFF73BF70L, "8\u7ebf|Line8");
            a.routeNamesEn.put(0xFF73BF70L, "8\u7ebf|Line8");
            a.routeNumbers.put(0xFF73BF70L, "8");
            a.exits = new String[]{"2"};
        } else if ("exitonly".equals(scenario)) {
            a.exits = new String[]{"3A"};
        } else if ("stationonly".equals(scenario)) {
            a.stationNames = new String[]{"\u89c2\u97f3\u6865||Guanyinqiao"};
        } else if ("longstation".equals(scenario)) {
            a.stationNames = new String[]{"重庆北站南广场||Chongqingbei Railway Station South Square"};
            a.colors = new long[]{0xFFF2A900L};
            a.routeNames.put(0xFFF2A900L, "环线");
            a.routeNamesEn.put(0xFFF2A900L, "Loop Line");
            a.routeNumbers.put(0xFFF2A900L, "");
            a.exits = new String[]{"5"};
            a.loops = new boolean[]{true};
        }
        return a;
    }

    // ---------- text layout (port of RenderCRTRailwaySign.renderCustomTextCRT) ----------
    static boolean containsCJK(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (Character.UnicodeScript.of(s.charAt(i)) == Character.UnicodeScript.HAN) return true;
        }
        return false;
    }

    static final class Fitted {
        BufferedImage img;
        int width, height, padding;
    }

    static Fitted fitted(String line, boolean bold, int fontSize, int color, double resScale) {
        Font base = bold ? FONT_SOURCE_HAN : FONT_ALIBABA;
        int scaled = Math.round((float) (fontSize * resScale));
        Font font = base.deriveFont(bold ? Font.BOLD : Font.PLAIN, (float) scaled);
        BufferedImage tmp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D tg = tmp.createGraphics();
        tg.setFont(font);
        FontMetrics fm = tg.getFontMetrics(font);
        int textWidth = fm.stringWidth(line);
        int asc = fm.getAscent(), desc = fm.getDescent();
        tg.dispose();
        int padding = Math.max(2, Math.round((float) (fontSize * 0.08F * resScale)));
        int tw = Math.max(1, textWidth + padding * 2);
        int th = Math.max(1, asc + desc + padding * 2);
        BufferedImage img = new BufferedImage(tw, th, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setFont(font);
        g.setColor(new Color(color, true));
        g.drawString(line, padding, padding + asc);
        g.dispose();
        Fitted f = new Fitted();
        f.img = img;
        f.width = tw;
        f.height = th;
        f.padding = padding;
        return f;
    }

    static float measureWidth(String text, float boxHeight) {
        if (text == null || text.isEmpty() || boxHeight <= 0) return 0;
        float resScale = (float) RES_SCALE;
        int totalGlyphHeight = 0, maxPxWidth = 0, lineCount = 0;
        for (String line : text.split("\\|")) {
            if (!line.isEmpty()) {
                boolean cjk = containsCJK(line);
                int fs = cjk ? 88 : 44;
                Fitted f = fitted(line, false, fs, 0xFF000000, resScale);
                if (f.width > 0 && f.height > 0) {
                    totalGlyphHeight += Math.max(1, f.height - f.padding * 2);
                    maxPxWidth = Math.max(maxPxWidth, f.width);
                    lineCount++;
                }
            }
        }
        if (lineCount == 0 || maxPxWidth == 0) return 0;
        float pixelToWorld = boxHeight / (float) totalGlyphHeight;
        return (float) maxPxWidth * pixelToWorld;
    }

    static final class LineRect {
        String text;
        double x, y, w, h; // world units
        Fitted fitted;
    }

    static void renderTextLikeJava(Graphics2D g, String text, double boxX, double boxY, double boxW, double boxH,
                                   int color, double scale, boolean bold, boolean centered, double pxPerWorld) {
        double resScale = RES_SCALE;
        if (text == null || text.isEmpty() || boxW <= 0 || boxH <= 0) return;
        java.util.List<String> lines = new ArrayList<>();
        for (String line : text.split("\\|")) if (!line.isEmpty()) lines.add(line);
        if (lines.isEmpty()) return;

        List<Fitted> fitted = new ArrayList<>();
        List<Integer> paddings = new ArrayList<>();
        List<Integer> raises = new ArrayList<>();
        int totalGlyphHeight = 0, maxPxWidth = 0, maxPadding = 0;
        for (String line : lines) {
            boolean cjk = containsCJK(line);
            int lineFontSize = cjk ? 88 : 44;
            Fitted f = fitted(line, bold, lineFontSize, color, resScale);
            if (f.width <= 0 || f.height <= 0) return;
            int padding = f.padding;
            fitted.add(f);
            paddings.add(padding);
            raises.add(!cjk ? Math.round((float) lineFontSize * 0.7F) : 0);
            totalGlyphHeight += Math.max(1, f.height - padding * 2);
            maxPxWidth = Math.max(maxPxWidth, f.width);
            maxPadding = Math.max(maxPadding, padding);
        }
        if (fitted.isEmpty()) return;
        double pixelToWorld = boxH / (double) totalGlyphHeight;
        double blockWidth = (double) maxPxWidth * pixelToWorld;
        double scaleY, scaleX;
        if (centered) {
            scaleY = scale;
            scaleX = Math.min(scale, boxW / blockWidth);
        } else {
            double fitScale = Math.min(1.0, boxW / blockWidth);
            scaleY = fitScale * scale;
            scaleX = fitScale * scale;
        }
        double width = blockWidth * scaleX;
        double height = (double) totalGlyphHeight * pixelToWorld * scaleY;
        double x1;
        double firstRaise = raises.isEmpty() ? 0 : (double) raises.get(0) * pixelToWorld * scaleY;
        double y1 = boxY + (boxH - height) / 2.0 + firstRaise / 2.0;
        if (centered) {
            double glyphWidth = (double) Math.max(1, maxPxWidth - maxPadding * 2) * pixelToWorld * scaleX;
            x1 = boxX + (boxW - glyphWidth) / 2.0;
        } else {
            x1 = boxX;
        }
        double lineY = y1;
        for (int i = 0; i < fitted.size(); i++) {
            Fitted f = fitted.get(i);
            int padding = paddings.get(i);
            int raise = raises.get(i);
            double lineW = (double) f.width * pixelToWorld * scaleX;
            double lineH = (double) f.height * pixelToWorld * scaleY;
            double lineX;
            if (centered) {
                double lineGlyphWidth = (double) Math.max(1, f.width - padding * 2) * pixelToWorld * scaleX;
                double lineCenterX = boxX + (boxW - lineGlyphWidth) / 2.0;
                lineX = lineCenterX - (double) padding * pixelToWorld * scaleX;
            } else {
                lineX = x1 - (double) padding * pixelToWorld * scaleX;
            }
            double finalLineY = lineY - (double) (padding + raise) * pixelToWorld * scaleY;
            int dx = (int) Math.round(lineX * pxPerWorld);
            int dy = (int) Math.round(finalLineY * pxPerWorld);
            int dw = (int) Math.round(lineW * pxPerWorld);
            int dh = (int) Math.round(lineH * pxPerWorld);
            if (dw > 0 && dh > 0) g.drawImage(f.img, dx, dy, dw, dh, null);
            lineY += (double) (Math.max(1, f.height - padding * 2)) * pixelToWorld * scaleY;
        }
    }

    // ---------- main ----------
    public static void main(String[] args) throws Exception {
        Map<String, String> a = new LinkedHashMap<>();
        for (int i = 0; i < args.length - 1; i += 2) a.put(args[i].replaceFirst("^--", ""), args[i + 1]);
        String scriptPath = a.getOrDefault("script", "_sign_ref/probe.js");
        String out = a.getOrDefault("out", "_sign_ref/out/probe.png");
        String scenario = a.getOrDefault("scenario", "train1");
        int cols = Integer.parseInt(a.getOrDefault("cols", "11"));
        double res = Double.parseDouble(a.getOrDefault("res", "0.75"));
        int px = Integer.parseInt(a.getOrDefault("px", "2048"));
        System.setProperty("java.awt.headless", "true");
        RES_SCALE = res;
        loadFonts();

        String script = new String(Files.readAllBytes(new File(scriptPath).toPath()), StandardCharsets.UTF_8);

        ScriptEngine engine = new NashornScriptEngineFactory().getScriptEngine(new String[]{"--language=es6"});
        engine.put("Text", new TextFactory());
        engine.put("Rect", new RectFactory());
        engine.put("Texture", new TexFactory());
        engine.put("Line", new LineFactory());
        engine.eval(script);

        double H = 0.5, W = cols * 0.5;
        SimApi api = buildApi(scenario, W, H);
        SimCtx ctx = new SimCtx(W, H);
        engine.put("api", api);
        Object state = engine.eval("({})");
        Invocable inv = (Invocable) engine;
        try { inv.invokeFunction("create", ctx, state, api); } catch (NoSuchMethodException ignored) { }
        inv.invokeFunction("render", ctx, state, api);

        int imgW = px;
        int imgH = (int) Math.round(px * H / W);
        BufferedImage img = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, imgW, imgH);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double pxPerWorld = imgW / W;
        for (Cmd c : ctx.cmds) c.paint(g, pxPerWorld);
        g.dispose();
        File f = new File(out);
        f.getParentFile().mkdirs();
        ImageIO.write(img, "png", f);
        System.out.println("commands=" + ctx.cmds.size() + " -> " + f.getAbsolutePath() + "  " + imgW + "x" + imgH);
        for (Cmd c : ctx.cmds) {
            if (c instanceof TextCmd) {
                TextCmd t = (TextCmd) c;
                System.out.printf("TEXT  box=(%.4f,%.4f,%.4f,%.4f) scale=%.2f bold=%b text=\"%s\"%n",
                        t.x, t.y, t.w, t.h, t.scale, t.bold, t.text);
            }
        }

        if (Arrays.asList(args).contains("--debug")) {
            double designPerPx = (DESIGN_H_PX / H) / pxPerWorld;
            System.out.println("--- debug: per-command ink bbox in DESIGN px (height basis 192) ---");
            int[] last = new int[4];
            for (int ci = 0; ci < ctx.cmds.size(); ci++) {
                Cmd c = ctx.cmds.get(ci);
                BufferedImage probe = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_RGB);
                Graphics2D pg = probe.createGraphics();
                pg.setColor(Color.WHITE);
                pg.fillRect(0, 0, imgW, imgH);
                pg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                pg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                c.paint(pg, pxPerWorld);
                pg.dispose();
                int minx = imgW, miny = imgH, maxx = -1, maxy = -1;
                for (int yy = 0; yy < imgH; yy++) {
                    for (int xx = 0; xx < imgW; xx++) {
                        int rgb = probe.getRGB(xx, yy);
                        int r = (rgb >> 16) & 0xFF, gg = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
                        if (r < 240 || gg < 240 || b < 240) {
                            if (xx < minx) minx = xx;
                            if (xx > maxx) maxx = xx;
                            if (yy < miny) miny = yy;
                            if (yy > maxy) maxy = yy;
                        }
                    }
                }
                String desc;
                double bx = 0, by = 0, bw = 0, bh = 0;
                if (c instanceof TextCmd) {
                    TextCmd t = (TextCmd) c;
                    desc = "TEXT \"" + t.text + "\" bold=" + t.bold + " sc=" + t.scale + " ctr=" + t.centered;
                    bx = t.x; by = t.y; bw = t.w; bh = t.h;
                } else if (c instanceof RectCmd) {
                    RectCmd t = (RectCmd) c;
                    desc = "RECT #" + Integer.toHexString(t.color);
                    bx = t.x; by = t.y; bw = t.w; bh = t.h;
                } else if (c instanceof TexCmd) {
                    TexCmd t = (TexCmd) c;
                    desc = "TEX " + t.tex;
                    bx = t.x; by = t.y; bw = t.w; bh = t.h;
                } else {
                    desc = "OTHER";
                }
                if (maxx < 0) {
                    System.out.printf("%2d %-46s NO-INK%n", ci, desc);
                    continue;
                }
                double ix0 = minx * designPerPx, iy0 = miny * designPerPx;
                double ix1 = (maxx + 1) * designPerPx, iy1 = (maxy + 1) * designPerPx;
                System.out.printf("%2d %-46s box=(%7.1f,%6.1f,%7.1f,%7.1f) ink=(%7.1f,%6.1f,%7.1f,%7.1f) inkH=%6.1f relTop=%7.4f relLeft=%7.4f relH=%7.4f%n",
                        ci, desc, bx * DESIGN_H_PX / H, by * DESIGN_H_PX / H, bw * DESIGN_H_PX / H, bh * DESIGN_H_PX / H,
                        ix0, iy0, ix1, iy1, iy1 - iy0,
                        (iy0 - by * DESIGN_H_PX / H) / (bh * DESIGN_H_PX / H),
                        (ix0 - bx * DESIGN_H_PX / H) / (bh * DESIGN_H_PX / H),
                        (iy1 - iy0) / (bh * DESIGN_H_PX / H));
                last = new int[]{minx, miny, maxx, maxy};
            }
        }
    }
}

