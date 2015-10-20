/*****************************************************
  DIVADid
  
  A document image degradation method.
  ------------------------------
  Author:
  2015 by Mathias Seuret <mathias.seuret@unifr.ch>
  -------------------

  This software is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation version 3.

  This software is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public
  License along with this software; if not, write to the Free Software
  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 ******************************************************************************/
package ch.unifr.diuf.diva.did.commands;

import ch.unifr.diuf.diva.did.Image;
import ch.unifr.diuf.diva.did.Script;
import java.io.IOException;
import org.jdom2.Element;
import org.jdom2.JDOMException;

/**
 *
 * @author Mathias Seuret
 */
public class MixImages extends AbstractCommand {

    public MixImages(Script script) {
        super(script);
    }

    @Override
    public float execute(Element task) throws IOException, JDOMException, InterruptedException {
        String refName = task.getAttributeValue("ref");
        if (refName==null) {
            throw new IllegalArgumentException(
                    "\n"+commandName+": ref is required"
            );
        }
        
        String cleanName = getChildString(task, "original");
        refName = script.preprocess(refName);
        
        if (!script.getImages().containsKey(refName)) {
            throw new IllegalArgumentException(
                    "\n"+commandName+", <apply-mix>: cannot find ref image "+refName
            );
        }
        if (!script.getImages().containsKey(cleanName)) {
            throw new IllegalArgumentException(
                    "\n"+commandName+", <apply-mix>: cannot find original image "+cleanName
            );
        }
        
        Image ref = script.getImages().get(refName);
        Image clean = script.getImages().get(cleanName);
        
        ref = mixImages(clean, ref);
        script.getImages().put(refName, ref);
        
        return 0;
    }
    
    private Image mixImages(Image original, Image noised) {
        if (original.getWidth()!=noised.getWidth() || original.getHeight()!=noised.getHeight()) {
            throw new IllegalArgumentException(
                    "\n"+commandName+": requires both images to have the same size"
            );
        }
        Image res = new Image(original.getWidth(), original.getHeight());
        
        for (int x=0; x<original.getWidth(); x++) {
            for (int y=0; y<original.getHeight(); y++) {
                float gray = (original.get(0, x, y)+original.get(2, x, y)+original.get(2, x, y))/3.0f;
                float ratio = 4 * gray * gray;
                if (ratio>1) {
                    ratio = 1;
                }
                for (int l=0; l<3; l++) {
                    float v = ratio*noised.get(l, x, y) + (1.0f-ratio)*original.get(l, x, y);
                    res.set(l, x, y, v);
                }
            }
        }
        
        return res;
    }
    
}
