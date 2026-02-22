package mp3;

import javax.swing.*;
import java.awt.*;

public class MII_Dialog extends JFrame {

    public MII_Dialog(String title, int width, int height) {
        setTitle(title); // 창 제목
        setSize(width, height); // 창 크기
        setLocation(400, 300);

        // 레이아웃
        setLayout(new BorderLayout());
        setResizable(false);

        Color[] colors = new Color[] {  new Color(255, 204, 0),
                                        new Color(255, 224, 45),
                                        new Color(255, 244, 90)
        };

        Font gullim = new Font("굴림", Font.BOLD, 15);

        JPanel panelBlBase = new JPanel(new GridLayout(3, 1));

        JPanel[] panelBls = new JPanel[]{new JPanel(new FlowLayout()),
                new JPanel(new FlowLayout()), new JPanel(new FlowLayout())};

        JLabel[] lblInfos = new JLabel[]{
                new JLabel("Seong Eun Song 1.0"),
                new JLabel("copyright by yang"),
                new JLabel("프로그램을 퍼가실 때에는"),
                new JLabel("출처를 남겨주시기 바랍니다.")};

        for (JLabel lblInfo : lblInfos) {
            lblInfo.setFont(gullim);
        }

        for (int i = 0; i < panelBls.length; i++) {
            panelBls[i].add(lblInfos[i]);
            if (i == 2) {
                panelBls[i].add(lblInfos[i + 1]);
            }
            panelBls[i].setBackground(colors[i]);
            panelBlBase.add(panelBls[i]);
        }

        add(panelBlBase);

    }
}
