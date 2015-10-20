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
import org.jdom2.Element;
import org.jdom2.JDOMException;

/**
 *
 * @author Mathias Seuret
 */
public abstract class AbstractGradientManipulation extends AbstractCommand {

    public AbstractGradientManipulation(Script script) {
        super(script);
    }
    
    public abstract void modifyGradient(Element task, GradientMap[] gradients, Image image) throws IOException;

    @Override
    public float execute(Element task) throws IOException, JDOMException, InterruptedException {
        // Read the noise level
        float boost  = 1;
        
        String imgName = script.preprocess(task.getAttributeValue("ref"));
        if (imgName==null) {
            throw new IllegalArgumentException(
                    "\n"+commandName+": requires a ref"
            );
        }
        if (!script.getImages().containsKey(imgName)) {
            throw new IllegalArgumentException(
                    "\n"+commandName+": cannot find image "+imgName
            );
        }
        Image image = script.getImages().get(imgName);
        
        
        // Read additional parameters
        int nbSteps = 500;
        float density = 1;
        final int SINGLE_CORE = 0;
        final int MULTI_CORES = 1;
        final int GPU = 2;
        int algoType = MULTI_CORES;
        int currentGPU = 0;
        for (Element child : task.getChildren()) {
            if (child.getName().equals("iterations")) {
                nbSteps = Integer.parseInt(script.preprocess(child.getText()));
                continue;
            }
            
            if (child.getName().equals("multi-core")) {
                algoType = MULTI_CORES;
                continue;
            }
            
            if (child.getName().equals("single-core")) {
                algoType = SINGLE_CORE;
                continue;
            }
            
            if (child.getName().equalsIgnoreCase("gpu")) {
                algoType = GPU;
                if (child.getText()!=null && !child.getText().equals("")) {
                    currentGPU = Integer.parseInt(child.getText());
                }
                continue;
            }
        }
        
        GradientMap[] grad = {
            new GradientMap(image, 0),
            new GradientMap(image, 1),
            new GradientMap(image, 2)
        };
        
        modifyGradient(task, grad, image);
        
        System.out.println("Starting reconstruction");
        for (int lvl=0; lvl<3; lvl++) {
            switch (algoType) {
                case SINGLE_CORE:
                    grad[lvl].CPUReconstruct(nbSteps);
                    break;
                case MULTI_CORES:
                    grad[lvl].MultiCPUReconstruct(nbSteps);
                    break;
                case GPU:
                    grad[lvl].GPUReconstruct(currentGPU, nbSteps);
                    break;
            }
        }
        for (int lvl=0; lvl<3; lvl++) {
            grad[lvl].pasteValues(image, lvl);
            grad[lvl] = null;
            System.gc();
            Thread.yield();
        }
        return 0;
    }
    
}
