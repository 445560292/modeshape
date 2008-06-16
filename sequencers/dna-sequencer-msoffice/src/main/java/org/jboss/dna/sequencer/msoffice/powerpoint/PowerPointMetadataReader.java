/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.sequencer.msoffice.powerpoint;

import java.util.List;
import java.io.*;
import java.awt.image.BufferedImage;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import org.apache.poi.hslf.usermodel.SlideShow;
import org.apache.poi.hslf.model.Slide;
import org.apache.poi.hslf.model.TextRun;

/**
 * Utility for extracting metadata from PowerPoint files
 * 
 * @author Michael Trezzi
 */
public class PowerPointMetadataReader {

    public static List<SlideMetadata> instance( InputStream stream ) throws IOException {
        SlideShow slideshow = new SlideShow(stream);
        Slide[] slides = slideshow.getSlides();

        List<SlideMetadata> slidesMetadata = new ArrayList<SlideMetadata>();

        for (Slide slide : slides) {
            SlideMetadata slideMetadata = new SlideMetadata();
            // process title
            slideMetadata.setTitle(slide.getTitle());

            // process notes
            for (TextRun textRun : slide.getNotesSheet().getTextRuns()) {
                if (slideMetadata.getNotes() == null) {
                    slideMetadata.setNotes("");
                }
                slideMetadata.setNotes(slideMetadata.getNotes() + textRun.getText());
            }
            // process text
            for (TextRun textRun : slide.getTextRuns()) {
                if (!textRun.getText().equals(slideMetadata.getTitle()) && textRun.getText() != null) {
                    if (slideMetadata.getText() == null) {
                        slideMetadata.setText("");
                    }
                    slideMetadata.setText(slideMetadata.getText() + textRun.getText());
                }
            }

            // process thumbnail
            Dimension pgsize = slideshow.getPageSize();

            BufferedImage img = new BufferedImage(pgsize.width, pgsize.height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = img.createGraphics();
            // clear the drawing area
            graphics.setPaint(Color.white);
            graphics.fill(new Rectangle2D.Float(0, 0, pgsize.width, pgsize.height));

            // render
            slide.draw(graphics);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(img, "png", out);
            slideMetadata.setThumbnail(out.toByteArray());

            slidesMetadata.add(slideMetadata);

        }

        return slidesMetadata;
    }

}
