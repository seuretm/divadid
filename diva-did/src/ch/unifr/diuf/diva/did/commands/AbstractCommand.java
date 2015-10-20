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
public abstract class AbstractCommand {
    protected Script script;
    protected String commandName;
    
    public AbstractCommand(Script script) {
        this.script = script;
    }
    
    public void setName(String s) {
        commandName = s;
    }
    
    private String getParameter(Element e, String childName) {
        Element child = e.getChild(childName);
        if (child==null) {
            illegalArgument("cannot find parameter "+childName+"\n"+e.toString());
        }
        if (child.getText()==null) {
            illegalArgument(childName+" cannot be empty");
        }
        return script.preprocess(child.getText());
    }
    
    private String getText(Element e) {
        return script.preprocess(e.getText());
    }
    
    public float getFloat(Element e) {
        return Float.parseFloat(script.preprocess(e.getText()));
    }
    
    public int getInt(Element e) {
        return Integer.parseInt(script.preprocess(e.getText()));
    }
    
    
    public int getChildInt(Element e, String childName) {
        return Integer.parseInt(getParameter(e, childName));
    }
    
    public int getChildInt(Element e, String childName, int def) {
        try {
            return getChildInt(e, childName);
        } catch (IllegalArgumentException ex) {
            return def;
        }
    }
    
    public float getChildFloat(Element e, String childName) {
        return Float.parseFloat(getParameter(e, childName));
    }
    
    public float getChildFloat(Element e, String childName, float def) {
        try {
            return getChildFloat(e, childName);
        } catch (IllegalArgumentException ex) {
            return def;
        }
    }
    
    public String getChildString(Element e, String childName) {
        return getParameter(e, childName);
    }
    
    public String getString(Element e) {
        return getText(e);
    }
    
    protected String getAttribute(Element e, String argName) {
        String arg = e.getAttributeValue(argName);
        if (arg==null) {
            illegalArgument("argument "+argName+" needed");
        }
        return script.preprocess(arg);
    }
    
    protected int getAttributeInt(Element e, String argName) {
        return Integer.parseInt(getAttribute(e, argName));
    }
    
    protected float getAttributeFloat(Element e, String argName) {
        return Float.parseFloat(getAttribute(e, argName));
    }
    
    protected void illegalArgument(String msg) {
        throw new IllegalArgumentException(
                "\n"+commandName+": "+msg
        );
    }
    
    public abstract float execute(Element task) throws IOException, JDOMException, InterruptedException;
    
}
