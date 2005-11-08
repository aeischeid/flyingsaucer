/*
 * {{{ header & license
 * Copyright (c) 2004, 2005 Joshua Marinacci, Torbj�rn Gannholm
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package org.xhtmlrenderer.render;

import org.xhtmlrenderer.css.constants.CSSName;
import org.xhtmlrenderer.css.style.CalculatedStyle;
import org.xhtmlrenderer.css.style.derived.RectPropertySet;
import org.xhtmlrenderer.layout.block.Relative;
import org.xhtmlrenderer.layout.content.ContentUtil;
import org.xhtmlrenderer.util.Configuration;
import org.xhtmlrenderer.util.GraphicsUtil;
import org.xhtmlrenderer.util.Uu;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;


/**
 * @author Joshua Marinacci
 * @author Torbj�rn Gannholm
 */
public class BoxRendering {

    public static void paint(RenderingContext c, Box box) {
        Box block = (Box) box;

        if (block instanceof AnonymousBlockBox) {
            InlineRendering.paintInlineContext(c, block);
        } else {
            CalculatedStyle calculatedStyle = box.getStyle().getCalculatedStyle();

            // copy the bounds to we don't mess it up
            Rectangle oldBounds = new Rectangle(c.getExtents());

            if (box.getStyle().isRelative()) {
                paintRelative(c, block);
            } else {
                paintNormal(c, block);
            }

            //Uu.p("here it's : " + c.getListCounter());
            if (ContentUtil.isListItem(calculatedStyle)) {
                paintListItem(c, box);
            }

            // move the origin down to account for the contents plus the margin, borders, and padding
            if (!box.getStyle().isAbsolute()) {
                oldBounds.y = oldBounds.y + block.height;
                c.setExtents(oldBounds);
            }
        }

        if (c.debugDrawBoxes() ||
                Configuration.isTrue("xr.renderer.debug.box-outlines", true)) {
            GraphicsUtil.drawBox(c.getGraphics(), block, Color.red);
        }
    }


    /**
     * Description of the Method
     *
     * @param c     PARAM
     * @param block PARAM
     */
    public static void paintNormal(RenderingContext c, Box block) {

        int width = block.getWidth();
        int height = block.getHeight();
        if (block.getState() != Box.DONE) {
            height += c.getCanvas().getHeight();
        }
        RectPropertySet margin = block.getStyle().getMarginWidth(c);

        // CLEAN: cast to int
        Rectangle bounds = new Rectangle(block.x + (int) margin.left(),
                block.y + (int) margin.top(),
                width - (int) margin.left() - (int) margin.right(),
                height - (int) margin.top() - (int) margin.bottom());
        paintBackground(c, block, bounds);

        //c.translateInsets(block);
        c.translate(block.tx, block.ty);
        /* Q&D for now if (block instanceof TableBox) {
            TableRendering.paintTable(c, (TableBox) block, restyle);
        } else */ if (isInlineLayedOut(block)) {
            InlineRendering.paintInlineContext(c, block);
        } else {
            BlockRendering.paintBlockContext(c, block);
        }
        c.translate(-block.tx, -block.ty);
        //c.untranslateInsets(block);

        BorderPainter.paint(bounds, BorderPainter.ALL,
                block.getStyle().getCalculatedStyle(), c.getGraphics(), c, 0);

    }

    // adjustments for relative painting
    /**
     * Description of the Method
     *
     * @param ctx   PARAM
     * @param block PARAM
     */
    public static void paintRelative(RenderingContext ctx, Box block) {
        Relative.translateRelative(ctx, block.getStyle().getCalculatedStyle());
        paintNormal(ctx, block);
        Relative.untranslateRelative(ctx, block.getStyle().getCalculatedStyle());
    }

    /**
     * Description of the Method
     *
     * @param c   PARAM
     * @param box PARAM
     */
    public static void paintBackground(RenderingContext c, Box box, Rectangle bounds) {
        Box block = box;

        // cache the background color
        //no sense getBackgroundColor(c);

        // get the css properties
        CalculatedStyle style = box.getStyle().getCalculatedStyle();
        String back_image = style.getStringProperty(CSSName.BACKGROUND_IMAGE);

        // load the background image
        block.background_image = null;
        int backImageWidth = 0;
        int backImageHeight = 0;
        if (back_image != null && !"none".equals(back_image)) {
            try {
                block.background_image = c.getUac().getImageResource(back_image).getImage();
                block.background_uri = back_image;
                backImageWidth = block.background_image.getWidth(null);
                backImageHeight = block.background_image.getHeight(null);
            } catch (Exception ex) {
                ex.printStackTrace();
                Uu.p(ex);
            }
        }

        // handle image positioning issues

        Point pt = style.getBackgroundPosition(bounds.width - backImageWidth, bounds.height - backImageHeight, c);
        block.background_position_horizontal = (int) pt.getX();
        block.background_position_vertical = (int) pt.getY();

        // actually paint the background
        BackgroundPainter.paint(c, block);
    }

    /**
     * Description of the Method
     *
     * @param c   PARAM
     * @param box PARAM
     */
    public static void paintListItem(RenderingContext c, Box box) {
        ListItemPainter.paint(c, box);
    }

    //TODO: check the logic here
    /**
     * Gets the inlineLayedOut attribute of the BoxRendering class
     *
     * @param box PARAM
     * @return The inlineLayedOut value
     */
    public static boolean isInlineLayedOut(Box box) {
        if (box.getChildCount() == 0) {
            return false;
        }//have to return something, it shouldn't matter
        for (int i = 0; i < box.getChildCount(); i++) {
            Box child = box.getChild(i);
            if (child instanceof LineBox) {
                return true;
            }
        }
        return false;
    }
}

/*
 * $Id$
 *
 * $Log$
 * Revision 1.59  2005/11/08 20:03:54  peterbrant
 * Further progress on painting order / improved positioning implementation
 *
 * Revision 1.58  2005/11/07 00:07:35  tobega
 * Got text-decoration and relative inlines kind-of working
 *
 * Revision 1.57  2005/11/05 18:45:06  peterbrant
 * General cleanup / Remove obsolete code
 *
 * Revision 1.56  2005/11/05 03:30:02  peterbrant
 * Start work on painting order and improved positioning implementation
 *
 * Revision 1.55  2005/11/03 17:58:41  peterbrant
 * Float rewrite (still stomping bugs, but demos work)
 *
 * Revision 1.54  2005/11/02 18:15:28  peterbrant
 * First merge of Tobe's and my stacking context work / Rework float code (not done yet)
 *
 * Revision 1.53  2005/11/01 23:49:24  tobega
 * Pulled floats and absolutes out of the "normal" rendering
 *
 * Revision 1.52  2005/10/30 00:02:35  peterbrant
 * - Minor cleanup to get rid of unused CssContext in Style constructor
 * - Switch to ArrayList from LinkedList in a few places (saves several MBs of memory on Hamlet)
 * - Introduce ScaledLineMetrics to work around apparent Java bug
 *
 * Revision 1.51  2005/10/29 22:31:01  tobega
 * House-cleaning
 *
 * Revision 1.50  2005/10/29 00:58:04  tobega
 * Split out restyling from rendering and fixed up hovering
 *
 * Revision 1.49  2005/10/27 00:09:03  tobega
 * Sorted out Context into RenderingContext and LayoutContext
 *
 * Revision 1.48  2005/10/22 22:58:15  peterbrant
 * Box level restyle works again (really this time!)
 *
 * Revision 1.47  2005/10/21 23:04:01  peterbrant
 * Make box level restyle work again
 *
 * Revision 1.46  2005/10/21 13:17:16  pdoubleya
 * Rename some methods in RectPropertySet, cleanup.
 *
 * Revision 1.45  2005/10/21 13:02:23  pdoubleya
 * Changed to cache padding in RectPropertySet.
 *
 * Revision 1.44  2005/10/21 12:01:20  pdoubleya
 * Added cachable rect property for margin, cleanup minor in styling.
 *
 * Revision 1.43  2005/10/18 20:57:05  tobega
 * Patch from Peter Brant
 *
 * Revision 1.42  2005/10/15 23:39:18  tobega
 * patch from Peter Brant
 *
 * Revision 1.41  2005/10/12 21:17:14  tobega
 * patch from Peter Brant
 *
 * Revision 1.40  2005/10/08 17:40:21  tobega
 * Patch from Peter Brant
 *
 * Revision 1.39  2005/10/02 21:30:00  tobega
 * Fixed a lot of concurrency (and other) issues from incremental rendering. Also some house-cleaning.
 *
 * Revision 1.38  2005/09/29 21:34:04  joshy
 * minor updates to a lot of files. pulling in more incremental rendering code.
 * fixed another resize bug
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
 * Revision 1.37  2005/09/26 22:40:21  tobega
 * Applied patch from Peter Brant concerning margin collapsing
 *
 * Revision 1.36  2005/07/26 22:05:02  joshy
 * fixed the fixed positioning rendering
 *
 * Revision 1.35  2005/07/21 01:10:34  joshy
 * fix for top abs pos bug and added new demo pages
 *
 * Revision 1.34  2005/07/20 22:47:33  joshy
 * fix for 94, percentage for top absolute position
 *
 * Revision 1.33  2005/07/20 18:11:41  joshy
 * bug fixes to absolute pos layout and box finding within abs layout
 *
 * Revision 1.32  2005/06/25 17:23:33  tobega
 * first refactoring of UAC: ImageResource
 *
 * Revision 1.31  2005/06/22 23:48:45  tobega
 * Refactored the css package to allow a clean separation from the core.
 *
 * Revision 1.30  2005/06/16 07:24:51  tobega
 * Fixed background image bug.
 * Caching images in browser.
 * Enhanced LinkListener.
 * Some house-cleaning, playing with Idea's code inspection utility.
 *
 * Revision 1.29  2005/06/05 01:02:35  tobega
 * Very simple and not completely functional table layout
 *
 * Revision 1.28  2005/06/03 19:56:43  tobega
 * Now uses first-line styles from all block-level ancestors
 *
 * Revision 1.27  2005/06/01 21:36:40  tobega
 * Got image scaling working, and did some refactoring along the way
 *
 * Revision 1.26  2005/05/29 19:37:58  tobega
 * Fixed up using different style borders.
 * Fixed patterned borders to work right.
 *
 * Revision 1.25  2005/05/17 06:56:25  tobega
 * Inline backgrounds now work correctly, as does mixing of inlines and blocks for style inheritance
 *
 * Revision 1.24  2005/05/13 15:23:55  tobega
 * Done refactoring box borders, margin and padding. Hover is working again.
 *
 * Revision 1.23  2005/05/13 11:49:59  tobega
 * Started to fix up borders on inlines. Got caught up in refactoring.
 * Boxes shouldn't cache borders and stuff unless necessary. Started to remove unnecessary references.
 * Hover is not working completely well now, might get better when I'm done.
 *
 * Revision 1.22  2005/05/12 23:42:03  tobega
 * Text decorations now work when set on block elements too
 *
 * Revision 1.21  2005/05/08 14:36:58  tobega
 * Refactored away the need for having a context in a CalculatedStyle
 *
 * Revision 1.20  2005/04/21 18:16:08  tobega
 * Improved handling of inline padding. Also fixed first-line handling according to spec.
 *
 * Revision 1.19  2005/04/19 17:51:18  joshy
 * fixed absolute positioning bug
 *
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
 * Revision 1.18  2005/03/26 11:53:30  pdoubleya
 * paintFixed() was badly refactored before--now again a duplicate of paintAbsolute; added check for absolute boxes on paint(), so that they are not moved after render operation.
 *
 *
 */
