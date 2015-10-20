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
public class SquareDiffVariance extends AbstractCommand {

    public SquareDiffVariance(Script script) {
        super(script);
    }

    @Override
    public float execute(Element task) throws IOException, JDOMException, InterruptedException {
        float var = 0.0f;
        
        String nameA = getChildString(task, "a");
        String nameB = getChildString(task, "b");
        
        if (!script.getImages().containsKey(nameA)) {
            throw new IllegalArgumentException(
                    commandName+":cannot find image \""+nameA+"\""
            );
        }
        
        if (!script.getImages().containsKey(nameB)) {
            throw new IllegalArgumentException(
                    commandName+":cannot find image \""+nameB+"\""
            );
        }
        
        Image a = script.getImages().get(nameA);
        Image b = script.getImages().get(nameB);
        
        float sum = 0.0f;
        Image c = new Image(a.getWidth(), b.getHeight());
        for (int l=0; l<3; l++) {
            for (int x=0; x<a.getWidth(); x++) {
                for (int y=0; y<a.getHeight(); y++) {
                    float d  = a.get(l, x, y) - b.get(l, x, y);
                    float dd = d*d;
                    c.set(l, x, y, dd);
                    sum += dd;
                }
            }
        }
        float mean = sum / (3*a.getWidth()*a.getHeight());
        
        for (int l=0; l<3; l++) {
            for (int x=0; x<a.getWidth(); x++) {
                for (int y=0; y<a.getHeight(); y++) {
                    float d = c.get(l, x, y)-mean;
                    var += d*d;
                }
            }
        }
        
        return var / (3*a.getWidth()*a.getHeight());
    }
    
}
