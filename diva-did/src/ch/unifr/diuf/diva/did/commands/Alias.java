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
public class Alias extends AbstractCommand {

    public Alias(Script script) {
        super(script);
    }

    @Override
    public float execute(Element e) throws IOException, JDOMException {
        String id = e.getAttributeValue("id");
        if (id==null) {
            throw new IllegalArgumentException(
                    "<alias> needs an id"
            );
        }
        
        if (e.getAttributeValue("value")==null) {
            if (!script.aliasExists(id)) {
                throw new IllegalStateException(
                        "The alias "+id+" has not been defined"
                );
            }
        } else {
            String val = getAttribute(e, "value");
            script.setAlias(id, val);
        }
        
        return 0;
    }
    
}
