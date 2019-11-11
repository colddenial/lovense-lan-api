package org.openstatic.lovense.swing;

import java.awt.Color;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JProgressBar;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.border.TitledBorder;
import java.awt.image.AffineTransformOp;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.HashMap;

import javax.swing.border.Border;
import org.openstatic.lovense.*;

public class LovenseToyCellRenderer extends JPanel implements ListCellRenderer<LovenseToy>
{
   private Border border;
   private JLabel toyLabel;
   private JPanel vibratePanel;
   private JProgressBar vibrate1Bar;
   private JProgressBar vibrate2Bar;
   private JProgressBar batteryBar;
   private HashMap<String, BufferedImage> icon_cache;

   public LovenseToyCellRenderer()
   {
      this.icon_cache = new HashMap<String, BufferedImage>();
      this.border = BorderFactory.createLineBorder(Color.RED, 1);

      this.setLayout(new BorderLayout());
      this.setOpaque(false);

      this.toyLabel = new JLabel();
      
      this.vibratePanel = new JPanel(new GridLayout(2,1,0,0));
      this.vibratePanel.setBorder(new TitledBorder("Vibrate"));
      this.vibratePanel.setOpaque(false);

      this.vibrate1Bar = new JProgressBar(0, 20);
      this.vibrate1Bar.setStringPainted(false);
      this.vibrate1Bar.setBorderPainted(false);
      this.vibrate1Bar.setOpaque(false);
      
      this.vibrate2Bar = new JProgressBar(0, 20);
      this.vibrate2Bar.setStringPainted(false);
      this.vibrate2Bar.setBorderPainted(false);
      this.vibrate2Bar.setOpaque(false);

      this.vibratePanel.add(this.vibrate1Bar);
      this.vibratePanel.add(this.vibrate2Bar);
      
      this.batteryBar = new JProgressBar(0,100);
      this.batteryBar.setStringPainted(true);
      this.batteryBar.setBorder(new TitledBorder("Battery"));
      this.batteryBar.setOpaque(false);

      this.add(this.toyLabel, BorderLayout.WEST);
      this.add(this.vibratePanel, BorderLayout.CENTER);
      this.add(this.batteryBar, BorderLayout.EAST);

   }

    public BufferedImage getIconForToy(String toy_name)
    {
        if (!this.icon_cache.containsKey(toy_name))
        {
          try
          {
              BufferedImage res_icon = ImageIO.read(getClass().getResource("/" + toy_name + ".png"));
              BufferedImage bi = resizeImage(64, res_icon);
              this.icon_cache.put(toy_name, bi);
          } catch (Exception e) {}
        }
        return this.icon_cache.get(toy_name);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends LovenseToy> list,
                                                 LovenseToy toy,
                                                 int index,
                                                 boolean isSelected,
                                                 boolean cellHasFocus)
    {
        //System.err.println("Rendering - " + toy.toString());
        this.toyLabel.setText("<html><body><b>" + toy.toString() + "</b><br />" + toy.getDevice().getHostname() + "<br />" + (toy.isConnected() ? "<font color=\"green\">Connected</font>" : "<font color=\"red\">Disconnected</font>") + "</body></html>");
        try
        {
          ImageIcon icon = new ImageIcon(this.getIconForToy(toy.getName()));
          this.toyLabel.setIcon(icon);
        } catch (Exception e) {
          e.printStackTrace(System.err);
        }

        int vibrate1 = toy.getOutputOneValue();
        int vibrate2 = toy.getOutputTwoValue();

        if (vibrate1 >= 0)
        {
            vibrate1Bar.setValue(vibrate1);
        } else {
            vibrate1Bar.setValue(0);
        }
        
        if (vibrate2 >= 0)
        {
            vibrate2Bar.setValue(vibrate2);
        } else {
            vibrate2Bar.setValue(0);
        }

        int batt = toy.getBattery();
        if (batt >= 0)
        batteryBar.setValue(batt);
        else
        batteryBar.setValue(0);


        if (isSelected)
        {
          this.setBackground(list.getSelectionBackground());
          this.setForeground(list.getSelectionForeground());
        } else {
          this.setBackground(list.getBackground());
          this.setForeground(list.getForeground());
        }

        this.setFont(list.getFont());
        this.setEnabled(list.isEnabled());

        if (isSelected)
          this.setBorder(border);
        else
          this.setBorder(null);
        //System.err.println("Finished Rendering");
        return this;
    }

    public static BufferedImage resizeImage(float square_size, BufferedImage in_image)
    {
        float scale_to_float = 0;
        float w = 0;
        float h = 0;
        float o_w = (float) in_image.getWidth();
        float o_h = (float) in_image.getHeight();
        int draw_x = 0;
        int draw_y = 0;
        if (w > h)
        {
            w = square_size;
            h = o_h * (square_size/o_w);
            draw_y = (int)((square_size / 2f) - (h/2f));
        } else {
            h = square_size;
            w = o_w * (square_size/o_h);
            draw_x = (int)((square_size / 2f) - (w/2f));
        }
        AffineTransform at = new AffineTransform();
        at.scale(w/o_w, h/o_h);
        AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);

        BufferedImage rgbi = new BufferedImage(in_image.getWidth(), in_image.getHeight(), BufferedImage.TYPE_INT_ARGB);

        rgbi.createGraphics().drawImage(in_image, 0, 0,  Color.WHITE, null);

        // Scale the image
        BufferedImage si = new BufferedImage((int)w, (int)h, BufferedImage.TYPE_INT_ARGB);
        scaleOp.filter(rgbi, si);

        // center the image
        BufferedImage ri = new BufferedImage((int)square_size, (int)square_size, BufferedImage.TYPE_INT_ARGB);
        ri.createGraphics().drawImage(si, draw_x, draw_y, Color.WHITE, null);
        return ri;
    }
}
