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

import ch.unifr.diuf.diva.did.commands.AbsDiff;
import ch.unifr.diuf.diva.did.commands.AbstractCommand;
import ch.unifr.diuf.diva.did.commands.Alias;
import ch.unifr.diuf.diva.did.commands.Calc;
import ch.unifr.diuf.diva.did.commands.CoOcMatrix;
import ch.unifr.diuf.diva.did.commands.CompareOrientations;
import ch.unifr.diuf.diva.did.commands.Correlation;
import ch.unifr.diuf.diva.did.commands.DecreaseMaxContrast;
import ch.unifr.diuf.diva.did.commands.DeleteImage;
import ch.unifr.diuf.diva.did.commands.FadeGradients;
import ch.unifr.diuf.diva.did.commands.GaussianNoise;
import ch.unifr.diuf.diva.did.commands.ImageCreator;
import ch.unifr.diuf.diva.did.commands.ManualGradientModification;
import ch.unifr.diuf.diva.did.commands.MixImages;
import ch.unifr.diuf.diva.did.commands.NoiseGradients;
import ch.unifr.diuf.diva.did.commands.NonNullMean;
import ch.unifr.diuf.diva.did.commands.PepperSalt;
import ch.unifr.diuf.diva.did.commands.Result;
import ch.unifr.diuf.diva.did.commands.SaveImage;
import ch.unifr.diuf.diva.did.commands.ScaleOffsetInvDist;
import ch.unifr.diuf.diva.did.commands.ScaledDifference;
import ch.unifr.diuf.diva.did.commands.SelectiveBlur;
import ch.unifr.diuf.diva.did.commands.SelectiveHVBlur;
import ch.unifr.diuf.diva.did.commands.SquareDiff;
import ch.unifr.diuf.diva.did.commands.TextPrinter;
import ch.unifr.diuf.diva.did.commands.Variance;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

/**
 * This class manages the execution of scripts.
 * @author Mathias Seuret
 */
public class Script {
    /**
     * Stores the script
     */
    Element root;
    
    String scriptName;
    
    Map<String, Image> images = new HashMap();
    
    Map<String, String> alias = new HashMap();
    
    Map<String, AbstractCommand> commands = new HashMap();
    
    float output;
    
    /**
     * Loads an XML script
     * @param fname file name of the script
     * @throws JDOMException if the XML is not correctly formated
     * @throws IOException if there is a problem to read the XML
     */
    public Script(String fname) throws JDOMException, IOException {
        SAXBuilder builder = new SAXBuilder();
        Document xml = builder.build(new File(fname));
        root = xml.getRootElement();
        scriptName = fname;
        
        addCommand("image", new ImageCreator(this));
        addCommand("alias", new Alias(this));
        addCommand("print", new TextPrinter(this));
        addCommand("apply-fade", new FadeGradients(this));
        addCommand("gradient-degradations", new NoiseGradients(this));
        addCommand("manual-gradient-degradations", new ManualGradientModification(this));
        addCommand("apply-mix", new MixImages(this));
        addCommand("save", new SaveImage(this));
        addCommand("delete", new DeleteImage(this));
        addCommand("pepper-salt", new PepperSalt(this));
        addCommand("gaussian-noise", new GaussianNoise(this));
        addCommand("selective-blur", new SelectiveBlur(this));
        addCommand("selective-hv-blur", new SelectiveHVBlur(this));
        addCommand("calc", new Calc(this));
        addCommand("result", new Result(this));
        addCommand("square-dist", new SquareDiff(this));
        addCommand("abs-dist", new AbsDiff(this));
        addCommand("gradient-diff", new CompareOrientations(this));
        addCommand("co-occurences", new CoOcMatrix(this));
        addCommand("non-null-mean", new NonNullMean(this));
        addCommand("variance", new Variance(this));
        addCommand("correlation", new Correlation(this));
        addCommand("scaled-difference", new ScaledDifference(this));
        addCommand("scale-offset-ind-dist", new ScaleOffsetInvDist(this));
        addCommand("decrease-max-contrast", new DecreaseMaxContrast(this));
    }
    
    public float getOutput() {
        return output;
    }
    
    public void run() throws IOException, InterruptedException, JDOMException {
        for (Element task : root.getChildren()) {
            String name = task.getName();
            
            if (!commands.containsKey(name)) {
                throw new IllegalArgumentException(
                        "Tag <"+name+"> not understood"
                );
            }
            output = commands.get(name).execute(task);
        }
    }
    
    public void setAlias(String key, String value) {
        alias.put(key, value);
    }
    
    public boolean aliasExists(String key) {
        return alias.containsKey(key);
    }
    
    public String preprocess(String in) {
        for (String key : alias.keySet()) {
            in = in.replaceAll(key, alias.get(key));
        }
        return in;
    }
    
    private void addCommand(String name, AbstractCommand cmd) {
        cmd.setName("<"+name+">");
        commands.put(name, cmd);
    }
    
    public Map<String, Image> getImages() {
        return images;
    }
}
