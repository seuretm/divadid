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

import ch.unifr.diuf.diva.did.Script;
import java.io.IOException;
import org.jdom2.Element;
import org.jdom2.JDOMException;

/**
 *
 * @author Mathias Seuret
 */
public class Calc extends AbstractCommand {

    public Calc(Script script) {
        super(script);
    }

    @Override
    public float execute(Element task) throws IOException, JDOMException, InterruptedException {
        float val = getAttributeFloat(task, "start");
        
        for (Element child : task.getChildren()) {
            if (child.getName().equals("add")) {
                val += getFloat(child);
                continue;
            }
            
            if (child.getName().equals("sub")) {
                val -= getFloat(child);
                continue;
            }
            
            if (child.getName().equals("multiply-by")) {
                val *= getFloat(child);
                continue;
            }
            
            if (child.getName().equals("divide-by")) {
                val /= getFloat(child);
                continue;
            }
            
            if (child.getName().equals("sqrt")) {
                val = (float)Math.sqrt(val);
                continue;
            }
            
            if (child.getName().equals("log")) {
                float base = (float)Math.exp(1);
                if (child.getAttributeValue("base")!=null) {
                    base = Float.parseFloat(child.getAttributeValue("base"));
                }
                val = (float)(Math.log(val) / Math.log(base));
                continue;
            }
            
            if (child.getName().equals("exp")) {
                val = (float)Math.exp(val);
                continue;
            }
            
            if (child.getName().equals("pow")) {
                float pow = getFloat(child);
                val = (float)Math.pow(val, pow);
                continue;
            }
        }
        
        return val;
    }
    
}
