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
public class SelectiveBlur extends AbstractCommand {

    public SelectiveBlur(Script script) {
        super(script);
    }

    @Override
    public float execute(Element task) throws IOException, JDOMException, InterruptedException {
        String refName  = task.getAttributeValue("ref");
        if (refName==null) {
            throw new IllegalArgumentException(
                    "\n"+commandName+": ref is required"
            );
        }
        refName = script.preprocess(refName);
        
        Element rangeElement = task.getChild("range");
        if (rangeElement==null) {
            throw new IllegalArgumentException(
                    "\n"+commandName+": <range> is required"
            );
        }
        int range = Integer.parseInt(script.preprocess(rangeElement.getText()));
        
        Element thresElement = task.getChild("threshold");
        if (thresElement==null) {
            throw new IllegalArgumentException(
                    "\n"+commandName+", <apply-selective-blur>: <range> is required"
            );
        }
        float thres = Float.parseFloat(script.preprocess(thresElement.getText()));
        thres = thres * thres;
        
        Image img = script.getImages().get(refName);
        Image res = new Image(img);
        
        for (int l=0; l<3; l++) {
            for (int x=0; x<res.getWidth(); x++) {
                for (int y=0; y<res.getHeight(); y++) {
                    float sum = 0;
                    int count = 0;
                    for (int dx=-range; dx<=range; dx++) {
                        for (int dy=-range; dy<=range; dy++) {
                            int px = x+dx;
                            int py = y+dy;
                            if (px<0 || py<0 || px>=res.getWidth() || py>=res.getHeight()) {
                                continue;
                            }
                            float dr = img.get(0,px,py)-img.get(0,x,y);
                            float dg = img.get(1,px,py)-img.get(1,x,y);
                            float db = img.get(2,px,py)-img.get(2,x,y);
                            if (dr*dr + dg*dg + db*db > thres) {
                                continue;
                            }
                            
                            sum += img.get(l, px, py);
                            count++;
                        }
                    }
                    res.set(l, x, y, sum/count);
                }
            }
        }
        script.getImages().put(refName, res);
        return 0;
    }
    
}
