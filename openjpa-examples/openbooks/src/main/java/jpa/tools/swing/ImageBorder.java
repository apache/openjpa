/*
 * Copyright 2010-2012 Pinaki Poddar
 *
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
*/
package jpa.tools.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;

import javax.swing.border.AbstractBorder;

@SuppressWarnings("serial")
public class ImageBorder extends AbstractBorder {

    Image image;
    String title;
    
//    Image topCenterImage, topLeftImage, topRight;
//
//    Image leftCenterImage, rightCenterImage;
//
//    Image bottomCenterImage, bottomLeftImage, bottomRightImage;

    Insets insets;

    public ImageBorder(Image img, String title) {
        image = img;
        this.title = title;
    }
//    public ImageBorder(Image top_left, Image top_center, Image top_right, Image left_center,
//        Image right_center, Image bottom_left, Image bottom_center, Image bottom_right) {
//
//      this.topLeftImage = top_left;
//      this.topCenterImage = top_center;
//      this.topRight = top_right;
//      this.leftCenterImage = left_center;
//      this.rightCenterImage = right_center;
//      this.bottomLeftImage = bottom_left;
//      this.bottomCenterImage = bottom_center;
//      this.bottomRightImage = bottom_right;
//    }

    public void setInsets(Insets insets) {
      this.insets = insets;
    }

    public Insets getBorderInsets(Component c) {
      if (insets != null) {
        return insets;
      } else {
//          return new Insets(topCenterImage.getHeight(null), leftCenterImage.getWidth(null), bottomCenterImage
//                  .getHeight(null), rightCenterImage.getWidth(null));
          return new Insets(image.getHeight(null), 100,10,100);
      }
    }

    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
      g.setColor(Color.white);
      g.fillRect(x, y, width, height);
      g.setColor(Color.black);
      
      Graphics2D g2 = (Graphics2D) g;
      int tlw = image.getWidth(null);
      int tlh = image.getHeight(null);
      fillTexture(g2, image, x, y, tlw, tlh);
      g2.drawString(title, tlw, 10);

//      int tlw = topLeftImage.getWidth(null);
//      int tlh = topLeftImage.getHeight(null);
//      int tcw = topCenterImage.getWidth(null);
//      int tch = topCenterImage.getHeight(null);
//      int trw = topRight.getWidth(null);
//      int trh = topRight.getHeight(null);
//
//      int lcw = leftCenterImage.getWidth(null);
//      int lch = leftCenterImage.getHeight(null);
//      int rcw = rightCenterImage.getWidth(null);
//      int rch = rightCenterImage.getHeight(null);
//
//      int blw = bottomLeftImage.getWidth(null);
//      int blh = bottomLeftImage.getHeight(null);
//      int bcw = bottomCenterImage.getWidth(null);
//      int bch = bottomCenterImage.getHeight(null);
//      int brw = bottomRightImage.getWidth(null);
//      int brh = bottomRightImage.getHeight(null);

//      fillTexture(g2, topLeftImage, x, y, tlw, tlh);
//      fillTexture(g2, topCenterImage, x + tlw, y, width - tlw - trw, tch);
//      fillTexture(g2, topRight, x + width - trw, y, trw, trh);
//
//      fillTexture(g2, leftCenterImage, x, y + tlh, lcw, height - tlh - blh);
//      fillTexture(g2, rightCenterImage, x + width - rcw, y + trh, rcw, height - trh - brh);
//
//      fillTexture(g2, bottomLeftImage, x, y + height - blh, blw, blh);
//      fillTexture(g2, bottomCenterImage, x + blw, y + height - bch, width - blw - brw, bch);
//      fillTexture(g2, bottomRightImage, x + width - brw, y + height - brh, brw, brh);
    }

    public void fillTexture(Graphics2D g2, Image img, int x, int y, int w, int h) {
      BufferedImage buff = createBufferedImage(img);
      Rectangle anchor = new Rectangle(x, y, img.getWidth(null), img.getHeight(null));
      TexturePaint paint = new TexturePaint(buff, anchor);
      g2.setPaint(paint);
      g2.fillRect(x, y, w, h);
    }

    public BufferedImage createBufferedImage(Image img) {
      BufferedImage buff = new BufferedImage(img.getWidth(null), img.getHeight(null),
          BufferedImage.TYPE_INT_ARGB);
      Graphics gfx = buff.createGraphics();
      gfx.drawImage(img, 0, 0, null);
      gfx.dispose();
      return buff;
    }
}

