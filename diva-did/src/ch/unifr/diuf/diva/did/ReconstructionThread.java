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

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This is the class used for multi-thread reconstructions, from the GradientMap
 * class.
 * @author Mathias Seuret
 */
public class ReconstructionThread extends Thread {
    private GradientMap gradientMap;
    private int id;
    private int nbCPU;
    Semaphore lock;
    AtomicBoolean isDone;
    Semaphore locLock;
    
    public ReconstructionThread(GradientMap map, int id, int nbCPU, Semaphore lock, AtomicBoolean isDone) {
        gradientMap = map;
        this.id     = id;
        this.lock   = lock;
        this.isDone = isDone;
        locLock     = new Semaphore(0);
        this.nbCPU  = nbCPU;
    }
    @Override
    public void run() {
        System.out.println("Thread "+id+":"+nbCPU+" started"); System.out.flush();
        for (int step=0; ; step++) {
            // Asks the permission to do the next step
            try {
                //lock.acquire();
                lock.tryAcquire(500, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ex) {
                //Logger.getLogger(ReconstructionThread.class.getName()).log(Level.SEVERE, null, ex);
                System.out.println("Semaphore acquisition failed");
            }
            if (isDone.get()==true) {
                System.out.println("Job "+id+" done");
                return;
            }
            
            switch (step%4) {
                case 0:
                    for (int y=2; y<gradientMap.height-2; y++) {
                        if (y%nbCPU==id) {
                            gradientMap.reconstructStepLR(y);
                        }
                    }
                    break;
                case 1:
                    for (int x=2; x<gradientMap.width-2; x++) {
                        if (x%nbCPU==id) {
                            gradientMap.reconstructStepTB(x);
                        }
                    }
                    break;
                case 2:
                    for (int y=2; y<gradientMap.height-2; y++) {
                        if (y%nbCPU==id) {
                            gradientMap.reconstructStepRL(y);
                        }
                    }
                    break;
                case 3:
                    for (int x=2; x<gradientMap.width-2; x++) {
                        if (x%nbCPU==id) {
                            gradientMap.reconstructStepBT(x);
                        }
                    }
                    break;
            }
            locLock.release();
            
        }
    }
    
    public void waitForReady() throws InterruptedException {
        locLock.tryAcquire(500, TimeUnit.MILLISECONDS);
    }
}
