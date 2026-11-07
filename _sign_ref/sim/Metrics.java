import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

/** Print AWT font metrics / string widths for the fonts used by JS signs. */
public final class Metrics {
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        Font alibaba = Font.createFont(Font.TRUETYPE_FONT, new File("fabric/src/main/resources/assets/nanbin/font/alibabapuhuiti_regular.ttf"));
        Font sourceHan = Font.createFont(Font.TRUETYPE_FONT, new File("fabric/src/main/resources/assets/nanbin/font/sourcehansanssc_bold.otf"));
        // default MTR dynamic texture resolution = 2 -> scale 0.75 -> CJK 88*0.75=66, latin 44*0.75=33
        int[] sizes = {66, 33, 88, 44};
        Font[] fonts = {alibaba, alibaba.deriveFont(Font.BOLD), sourceHan, sourceHan.deriveFont(Font.BOLD)};
        String[] names = {"ALIBABA", "ALIBABA+BOLD", "SOURCEHAN", "SOURCEHAN+BOLD"};
        String[] samples = {"3", "9A", "10", "号线", "出入口", "环", "线", "Line", "Entrance", "Station", "地铁站",
                "Loop Line", "Xiaoshizi", "地铁站||Station", "小什字||Xiaoshizi", "号线|Line", "出入口|Entrance"};
        for (int fi = 0; fi < fonts.length; fi++) {
            for (int si : sizes) {
                Font font = fonts[fi].deriveFont((float) si);
                BufferedImage tmp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = tmp.createGraphics();
                g.setFont(font);
                FontMetrics fm = g.getFontMetrics(font);
                System.out.printf("%s size=%d  asc=%d desc=%d height=%d  asc/em=%.4f desc/em=%.4f (asc+desc)/em=%.4f%n",
                        names[fi], si, fm.getAscent(), fm.getDescent(), fm.getHeight(),
                        fm.getAscent() / (double) si, fm.getDescent() / (double) si,
                        (fm.getAscent() + fm.getDescent()) / (double) si);
                for (String s : samples) {
                    int width = fm.stringWidth(s);
                    System.out.printf("    %-20s width=%5d  width/em=%8.4f%n", "\"" + s + "\"", width, width / (double) si);
                }
                g.dispose();
            }
        }
    }
}
