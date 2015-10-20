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
public class CoOcMatrix extends AbstractCommand {

    public CoOcMatrix(Script script) {
        super(script);
    }

    @Override
    public float execute(Element task) throws IOException, JDOMException, InterruptedException {
        String ref = getAttribute(task, "ref");
        int size   = getChildInt(task, "size");
        String img = getChildString(task, "image");
        
        if (!script.getImages().containsKey(img)) {
            throw new IllegalArgumentException(
                    commandName+": cannot find image "+img
            );
        }
        Image i = script.getImages().get(img);
        
        Image mat = new Image(size, size);
        int S     = size-1;
        for (int x=0; x<i.getWidth()-1; x++) {
            for (int y=0; y<i.getHeight()-1; y++) {
                for (int l=0; l<3; l++) {
                    int t = (int)(S*i.get(l, x, y));
                    int u = (int)(S*i.get(l, x+1, y));
                    int v = (int)(S*i.get(l, x, y+1));
                    int w = (int)(S*i.get(l, x+1, y+1));
                    mat.set(0, t, u, mat.get(0, t, u)+1);
                    mat.set(1, t, v, mat.get(1, t, v)+1);
                    mat.set(2, t, w, mat.get(2, t, w)+1);
                }
            }
        }
        
        for (int l=0; l<3; l++) {
            float max = 0.0f;
            for (int x=0; x<size; x++) {
                for (int y=0; y<size; y++) {
                    if (mat.get(l,x,y)>max) {
                        max = mat.get(l,x,y);
                    }
                }
            }
            for (int x=0; x<size; x++) {
                for (int y=0; y<size; y++) {
                    mat.set(l, x, y, mat.get(l, x, y)/max);
                }
            }
        }
        
        script.getImages().put(ref, mat);
        
        return 0;
    }
    
}
