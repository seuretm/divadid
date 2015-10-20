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
public class Variance extends AbstractCommand {

    public Variance(Script script) {
        super(script);
    }

    @Override
    public float execute(Element task) throws IOException, JDOMException, InterruptedException {
        String ref = getAttribute(task, "ref");
        
        if (!script.getImages().containsKey(ref)) {
            throw new IllegalArgumentException(
                    commandName+": cannot find image "+ref
            );
        }
        Image i = script.getImages().get(ref);
        
        float meanVar = 0;
        for (int l=0; l<3; l++) {
            float sum = 0;
            int count = 0;
            for (int x=0; x<i.getWidth(); x++) {
                for (int y=0; y<i.getHeight(); y++) {
                    sum += i.get(l, x, y);
                    count++;
                }
            }
            float mean = sum / count;
            sum = 0;
            count = 0;
            for (int x=0; x<i.getWidth(); x++) {
                for (int y=0; y<i.getHeight(); y++) {
                    float d = mean-i.get(l, x, y);
                    sum += d*d;
                    count++;
                }
            }
            float var = sum / count;
            meanVar += var;
        }
        
        
        return meanVar / 3;
    }
    
}
