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
public class ImageCreator extends AbstractCommand {

    public ImageCreator(Script script) {
        super(script);
    }

    @Override
    public float execute(Element task) throws IOException, JDOMException {
        String id = getAttribute(task, "id");
        if (id==null) {
            throw new IllegalArgumentException(
                    "\n"+commandName+": requires an id"
            );
        }
        
        boolean alreadyInitialized = false;
        Image img = null;
        for (Element param : task.getChildren()) {
            if (param.getName().equals("load")) {
                if (alreadyInitialized) {
                    throw new IllegalArgumentException(
                            "\n"+commandName+": cannot be created twice"
                    );
                }
                String fname = param.getAttributeValue("file");
                img = loadImage(fname);
                alreadyInitialized = true;
                continue;
            }
            
            if (param.getName().equals("copy")) {
                if (alreadyInitialized) {
                    throw new IllegalArgumentException(
                            "\n"+commandName+", <image> cannot be created twice"
                    );
                }
                String otherName = param.getAttributeValue("ref");
                img = copyImage(otherName);
                alreadyInitialized = true;
                continue;
            }
            
            if (param.getName().equals("diff")) {
                if (alreadyInitialized) {
                    throw new IllegalArgumentException(
                            "\n"+commandName+", <image> cannot be created twice"
                    );
                }
                String a = getChildString(param, "a");
                String b = getChildString(param, "b");
                Image ia = script.getImages().get(a);
                Image ib = script.getImages().get(b);
                img = new Image(ia.getWidth(), ia.getHeight());
                for (int x=0; x<ia.getWidth(); x++) {
                    for (int y=0; y<ia.getHeight(); y++) {
                        img.set(0, x, y, ia.get(0, x, y)-ib.get(0, x, y));
                        img.set(1, x, y, ia.get(1, x, y)-ib.get(1, x, y));
                        img.set(2, x, y, ia.get(2, x, y)-ib.get(2, x, y));
                    }
                }
                alreadyInitialized = true;
                continue;
            }
        }
        script.getImages().put(id, img);
        return 0;
    }
    
    private Image loadImage(String fname) throws IOException {
         if (fname==null) {
            throw new IllegalArgumentException(
                    "\n"+commandName+", <load> needs a file"
            );
        }
        fname = script.preprocess(fname);
        return new Image(fname);
    }
    
    private Image copyImage(String otherName) {
        if (otherName==null) {
            throw new IllegalArgumentException(
                    "\n"+commandName+", <copy> requires a ref"
            );
        }
        otherName = script.preprocess(otherName);
        if (!script.getImages().containsKey(otherName)) {
            throw new IllegalArgumentException(
                    "\n"+commandName+", <copy>: cannot find image "+otherName
            );
        }
        return new Image(script.getImages().get(otherName));
    }
    
}
