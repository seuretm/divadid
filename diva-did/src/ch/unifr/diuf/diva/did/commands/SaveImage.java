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
public class SaveImage extends AbstractCommand {

    public SaveImage(Script script) {
        super(script);
    }

    @Override
    public float execute(Element task) throws IOException, JDOMException, InterruptedException {
        String refName  = task.getAttributeValue("ref");
        String fileName = task.getAttributeValue("file");
        if (refName==null) {
            throw new IllegalArgumentException(
                    "\n"+commandName+": ref is required"
            );
        }
        if (fileName==null) {
            throw new IllegalArgumentException(
                    "\n"+commandName+": file is required"
            );
        }
        refName = script.preprocess(refName);
        fileName = script.preprocess(fileName);
        
        if (!script.getImages().containsKey(refName)) {
            throw new IllegalArgumentException(
                    "\n"+commandName+", <save>: cannot find ref image "+refName
            );
        }
        Image ref = script.getImages().get(refName);
        ref.write(fileName);
        return 0;
    }
    
}
