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
package ch.unifr.diuf.diva.did;


import java.io.IOException;
import org.jdom2.JDOMException;

/**
 *
 * @author Mathias Seuret
 */
public class DivaDid {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, InterruptedException, JDOMException {
        Script script = null;
        
        for (String s : args) {
            if (script == null) {
                script = new Script(s);
                continue;
            }

            int eqPos = s.indexOf("=");
            if (eqPos <= 0) {
                throw new IllegalArgumentException(
                        "Cannot understand " + s + "\n"
                        + "Arguments shouldb be passed as key=value \n"
                        + "See documentation."
                );
            }
            script.setAlias(s.substring(0, eqPos), s.substring(eqPos + 1, s.length()));
        }

        script.run();
    }
}
