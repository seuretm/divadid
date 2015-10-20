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

import ch.unifr.diuf.diva.did.GradientMap;
import ch.unifr.diuf.diva.did.Image;
import ch.unifr.diuf.diva.did.Script;
import java.io.IOException;
import java.util.List;
import org.jdom2.Element;

/**
 *
 * @author Mathias Seuret
 */
public class ManualGradientModification extends AbstractGradientManipulation {

    public ManualGradientModification(Script script) {
        super(script);
    }

    @Override
    public void modifyGradient(Element task, GradientMap[] grad, Image image) throws IOException {
        List<Element> degradations = task.getChildren("degradation");
        for (Element d : degradations) {
            float strength = getChildFloat(d, "strength");
            int x = getChildInt(d, "x");
            int y = getChildInt(d, "y");
            String source  = getChildString(d, "file");
            
            Image img = new Image(source);
            for (int lvl=0; lvl<3; lvl++) {
                GradientMap n = new GradientMap(img, lvl);
                n.pasteGradient(image, grad[lvl], x, y, strength);
            }
            
        }
    }
    
}
