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
public class ScaledDifference extends AbstractCommand {

    public ScaledDifference(Script script) {
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
        
        float sum    = 0;
        long count   = a.getWidth()*a.getHeight();
        float delta2 = 0;
        for (int channel=0; channel<3; channel++) {
            // Computing the means
            float sumA = 0;
            float sumB = 0;
            for (int x=0; x<a.getWidth(); x++) {
                for (int y=0; y<a.getHeight(); y++) {
                    sumA += a.get(channel, x, y);
                    sumB += b.get(channel, x, y);
                }
            }
            float alpha = sumA / count;
            float beta  = sumB / count;
            
            // Computing eta
            float top = 0;
            float bot = 0;
            for (int x=0; x<a.getWidth(); x++) {
                for (int y=0; y<a.getHeight(); y++) {
                    top += (a.get(channel, x, y)-alpha)*(b.get(channel, x, y)-beta);
                    bot += (a.get(channel, x, y)-alpha)*(a.get(channel, x, y)-alpha);
                }
            }
            float eta = top / bot;
            
            for (int x=0; x<a.getWidth(); x++) {
                for (int y=0; y<a.getHeight(); y++) {
                    float d = eta*(a.get(channel, x, y)-alpha) - (b.get(channel, x, y)-beta);                    
                    delta2 += d*d;
                }
            }
        }
        
        return (float)Math.sqrt(delta2 / count / 3);
    }
    
}
