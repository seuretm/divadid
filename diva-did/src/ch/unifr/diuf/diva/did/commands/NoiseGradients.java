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
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.jdom2.Element;

/**
 *
 * @author Mathias Seuret
 */
public class NoiseGradients extends AbstractGradientManipulation {

    public NoiseGradients(Script script) {
        super(script);
    }

    @Override
    public void modifyGradient(Element task, GradientMap[] grad, Image image) throws IOException {
        float density  = getChildFloat(task, "density");
        float strength = getChildFloat(task, "strength");
        String source  = getChildString(task, "source");
        
        File folder = new File(source);
        File[] listOfFiles = folder.listFiles();
        
        System.out.println("Computing mean patch surface");
        long patchSurf = 0;
        for (File f : listOfFiles) {
            BufferedImage bi = ImageIO.read(new File(f.getCanonicalPath()));
            int surf = Math.max(0, (bi.getWidth()-20)*(bi.getHeight()-20));
            patchSurf += surf;
        }
        float sP = patchSurf / listOfFiles.length;
        float sD = (image.getWidth()*image.getHeight());
        float N  = listOfFiles.length;
        float b  = sP / sD;
        float proba     = 1 - 1 / (density/(N*b)+1);
        
        int count = 0;
        for (File f : listOfFiles) {
            Image img = new Image(f.getCanonicalPath());
            double rnd = Math.random();
            for (int i=0; rnd<proba && i<N; i++) {
                if (i>0) {
                    img.turnLeft();
                }
                int px = -img.getWidth()  + (int)(Math.random()*(image.getWidth()+2*img.getWidth()));
                int py = -img.getHeight() + (int)(Math.random()*(image.getHeight()+2*img.getHeight()));
                for (int lvl=0; lvl<3; lvl++) {
                    GradientMap n = new GradientMap(img, lvl);
                    n.pasteGradient(image, grad[lvl], px, py, strength);
                }
                count++;
                rnd = Math.random();
            }
        }
        System.out.println(count+" patches pasted");
        System.out.println("Patches per pixel: "+(count*sP/sD));
    }
    
}
