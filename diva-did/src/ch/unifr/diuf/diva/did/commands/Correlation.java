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
public class Correlation extends AbstractCommand {

    public Correlation(Script script) {
        super(script);
    }

    @Override
    public float execute(Element task) throws IOException, JDOMException, InterruptedException {
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
        
        if (a.getWidth()!=b.getWidth() || a.getHeight()!=b.getHeight()) {
            throw new IllegalArgumentException(
                    commandName+": both images must have the same dimension"
            );
        }
        
        float var = 0;
        int nb    = a.getWidth() * a.getHeight();
        for (int c=0; c<3; c++) {
            float sumA = 0;
            float sumB = 0;
            for (int x=0; x<a.getWidth(); x++) {
                for (int y=0; y<a.getHeight(); y++) {
                    sumA += a.get(c, x, y);
                    sumB += b.get(c, x, y);
                }
            }
            float meanA = sumA / nb;
            float meanB = sumB / nb;
            float top   = 0;
            float left  = 0;
            float right = 0;
            for (int x=0; x<a.getWidth(); x++) {
                for (int y=0; y<a.getHeight(); y++) {
                    top   += (a.get(c, x, y)-meanA)*(b.get(c, x, y)-meanB);
                    left  += (a.get(c, x, y)-meanA)*(a.get(c, x, y)-meanA);
                    right += (b.get(c, x, y)-meanB)*(b.get(c, x, y)-meanB);
                }
            }
            var += top / (Math.sqrt(left)*Math.sqrt(right));
        }
        
        return var / 3;
    }
    
    
}
