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
public class PepperSalt extends AbstractCommand {

    public PepperSalt(Script script) {
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
        refName = script.preprocess(refName);
        
        if (!script.getImages().containsKey(refName)) {
            throw new IllegalArgumentException(
                    "\n"+commandName+": cannot find ref image "+refName
            );
        }
        Image img = script.getImages().get(refName);
        
        float noiseQuantity = getChildFloat(task, "fraction");
        
        for (int x=0; x<img.getWidth(); x++) {
            for (int y=0; y<img.getHeight(); y++) {
                if (Math.random()>noiseQuantity) {
                    continue;
                }
                float color = (Math.random()<0.5) ? 0 : 1;
                for (int d=0; d<img.getDepth(); d++) {
                    img.set(d, x, y, color);
                }
            }
        }
        return 0;
    }
    
}
