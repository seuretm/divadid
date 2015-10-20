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
import java.util.concurrent.atomic.AtomicBoolean;
import org.jocl.CL;
import static org.jocl.CL.CL_CONTEXT_PLATFORM;
import static org.jocl.CL.CL_DEVICE_TYPE_ALL;
import static org.jocl.CL.CL_MEM_COPY_HOST_PTR;
import static org.jocl.CL.CL_MEM_READ_ONLY;
import static org.jocl.CL.CL_MEM_READ_WRITE;
import static org.jocl.CL.CL_PLATFORM_NAME;
import static org.jocl.CL.CL_TRUE;
import static org.jocl.CL.clBuildProgram;
import static org.jocl.CL.clCreateBuffer;
import static org.jocl.CL.clCreateCommandQueue;
import static org.jocl.CL.clCreateContext;
import static org.jocl.CL.clCreateKernel;
import static org.jocl.CL.clCreateProgramWithSource;
import static org.jocl.CL.clEnqueueNDRangeKernel;
import static org.jocl.CL.clEnqueueReadBuffer;
import static org.jocl.CL.clGetDeviceIDs;
import static org.jocl.CL.clGetPlatformIDs;
import static org.jocl.CL.clGetPlatformInfo;
import static org.jocl.CL.clReleaseCommandQueue;
import static org.jocl.CL.clReleaseContext;
import static org.jocl.CL.clReleaseKernel;
import static org.jocl.CL.clReleaseMemObject;
import static org.jocl.CL.clReleaseProgram;
import static org.jocl.CL.clSetKernelArg;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_context_properties;
import org.jocl.cl_device_id;
import org.jocl.cl_event;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;
import org.jocl.cl_platform_id;
import org.jocl.cl_program;

/**
 * This class represents the channel of an image with its gradients.
 * It also stores the original values for reconstruction purposes.
 * It also has methods for reconstructing the values from the gradients,
 * either using a single thread, several threads or a GPU.
 * Take note that for using the GPU, openCL has to be installed and
 * working.
 * @author Mathias Seuret
 */
public class GradientMap {
    
    float[][] gx;
    float[][] gy;
    float[][] val;
    int width;
    int height;

    public GradientMap(Image img, int channelNum) {
        width = img.getWidth();
        height = img.getHeight();
        gx = new float[width][height];
        gy = new float[width][height];
        val = new float[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                val[x][y] = img.get(channelNum, x, y);
            }
        }
        for (int x = 0; x < width - 1; x++) {
            for (int y = 0; y < height - 1; y++) {
                gx[x][y] = val[x + 1][y] - val[x][y];
                gy[x][y] = val[x][y + 1] - val[x][y];
            }
        }
    }
    
    public float[][] getGX() {
        return gx;
    }
    
    public float[][] getGY() {
        return gy;
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }

    /**
     * Evaluates how far the values are from the gradient.
     * @return 
     */
    public float getError() {
        float err = 0.0f;

        for (int x = 0; x < width - 1; x++) {
            for (int y = 0; y < height - 1; y++) {
                err += Math.abs(gx[x][y] - (val[x + 1][y] - val[x][y]));
                err += Math.abs(gy[x][y] - (val[x][y + 1] - val[x][y]));
            }
        }

        return err;
    }

    /**
     * Pastes the (reconstructed?) values onto an image.
     * @param target image
     * @param channel color
     * @return the difference between the original data and the pasted values
     */
    public float pasteValues(Image target, int channel) {
        float diff = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (target.getValues(x, y)[0]<0.1 && target.getValues(x, y)[1]<0.1 && target.getValues(x, y)[2]<0.1) {
                    continue;
                }
                diff += (float)Math.abs(target.getValues(x,y)[channel]-val[x][y]);
                target.set(channel, x, y, val[x][y]);
            }
        }
        return diff;
    }
    
    public void pasteGradient(Image img, GradientMap target, int x, int y, float boost) {
        for (int dx = 0; dx < width; dx++) {
            int px = x + dx;
            if (px >= target.width - 1 || px<0) {
                continue;
            }
            for (int dy = 0; dy < height; dy++) {
                int py = y + dy;
                if (py >= target.height - 1 || py<0) {
                    continue;
                }
                if (img.getValues(px, py)[0]<0.025 && img.getValues(px, py)[1]<0.025 && img.getValues(px, py)[2]<0.025) {
                    continue;
                }
                float gxb = Math.abs(gx[dx][dy]*boost);
                float gyb = Math.abs(gy[dx][dy]*boost);
                float tx  = Math.abs(target.gx[px][py]);
                float ty  = Math.abs(target.gy[px][py]);
                if (gxb*gxb+gyb*gyb > tx*tx+ty*ty) {
                    target.gx[px][py] = gx[dx][dy]*boost;
                    target.gy[px][py] = gy[dx][dy]*boost;
                }
            }
        }
    }

    public float[] getValues1D() {
        float[] res = new float[width * height];
        int p = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                res[p++] = val[x][y];
            }
        }
        return res;
    }

    public void setValues1D(float[] in) {
        int p = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                val[x][y] = in[p++];
            }
        }
    }

    public float[] getGX1D() {
        float[] res = new float[width * height];
        int p = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                res[p++] = gx[x][y];
            }
        }
        return res;
    }

    public float[] getGY1D() {
        float[] res = new float[width * height];
        int p = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                res[p++] = gy[x][y];
            }
        }
        return res;
    }

    protected void reconstructStepLR() {
        for (int y = 2; y<height-2; y++) {
            reconstructStepLR(y);
        }
    }
    
    protected void reconstructStepLR(int y) {
        for (int x=2; x<width-2; x++) {
            val[x][y] = (val[x][y] + val[x - 1][y] + gx[x - 1][y]) / 2;
        }
    }

    protected void reconstructStepRL() {
        for (int y=2; y<height-2; y++) {
            reconstructStepRL(y);
        }
    }
    
    protected void reconstructStepRL(int y) {
        for (int x=width-3; x>=2; x--) {
            val[x][y] = (val[x][y] + val[x + 1][y] - gx[x][y]) / 2;
        }
    }

    protected void reconstructStepTB() {
        for (int x=2; x<width-2; x++) {
            reconstructStepTB(x);
        }
    }
    
    protected void reconstructStepTB(int x) {
        for (int y=2; y<height-2; y++) {
            val[x][y] = (val[x][y] + val[x][y - 1] + gy[x][y - 1]) / 2;
        }
    }

    protected void reconstructStepBT() {
        for (int x=2; x<width-2; x++) {
            reconstructStepBT(x);
        }
    }
    
    protected void reconstructStepBT(int x) {
        for (int y=height-3; y>=2; y--) {
            val[x][y] = (val[x][y] + val[x][y + 1] - gy[x][y]) / 2;
        }
    }

    /**
     * If you want to understand how the method works, this is the method
     * you should look at. The other ones do the same, but in more complicated
     * ways.
     * @param nbSteps 
     */
    public void CPUReconstruct(int nbSteps) {
        long start = System.currentTimeMillis();
        for (int step=0; step<nbSteps; step++) {
            switch (step%4) {
                case 0:
                    reconstructStepLR();
                    break;
                case 1:
                    reconstructStepTB();
                    break;
                case 2:
                    reconstructStepRL();
                    break;
                case 3:
                    reconstructStepBT();
                    break;
            }
        }
        long compTime = System.currentTimeMillis() - start;
        long workMem = width*height*4;
        long nbOp    = nbSteps * width;
        long taskMem = (width+height)/2*4;
        System.out.println("result;single-core;"+workMem+";"+taskMem+";"+nbOp+";"+0+";"+compTime);
    }
    
    public void MultiCPUReconstruct(int nbSteps) throws InterruptedException {
        long start = System.currentTimeMillis();
        int nbCPU = Runtime.getRuntime().availableProcessors();
        Semaphore lock = new Semaphore(0);
        AtomicBoolean  isDone = new AtomicBoolean(false);
        
        long initTime = System.currentTimeMillis() - start;
        
        System.out.println("Starting reconstruction with "+nbCPU+" CPU");
        System.out.flush();
        
        // Creates & starts threads
        ReconstructionThread[] thread = new ReconstructionThread[nbCPU];
        for (int cpu=0; cpu<nbCPU; cpu++) {
            thread[cpu] = new ReconstructionThread(this, cpu, nbCPU, lock, isDone);
            thread[cpu].start();
        }
        
        for (int step=0; step<nbSteps; step++) {
            lock.release(nbCPU);
            
            for (int cpu=0; cpu<nbCPU; cpu++) {
                thread[cpu].waitForReady();
            }
            
            if (step%100==0) {
                long chrono    = System.currentTimeMillis() - start;
                long remaining = 1+(long)(chrono/(step+1.0f) * (nbSteps-step) / 1000);
                System.out.print("\r"+step+"/"+nbSteps+", "+remaining+" seconds remaining");
                System.out.flush();
            }
        }
        System.out.println("");
        
        isDone.set(true);
        lock.release(nbCPU);
        
        long compTime = System.currentTimeMillis() - start;
        long workMem = width*height*4;
        long nbOp    = nbSteps * width;
        long taskMem = (width+height)/2*4;
        System.out.println("result;multi-cpu;"+workMem+";"+taskMem+";"+nbOp+";"+initTime+";"+compTime);
    }

    public void GPUReconstruct(int currentGPU, int nbSteps) {
        long chrono = System.currentTimeMillis();
        
        // Get the data
        float[] gx = getGX1D();
        float[] gy = getGY1D();
        float[] val = getValues1D();

        // Initialization of JOCL
        final int PLATFORM_INDEX = currentGPU;
        final long DEVICE_TYPE = CL_DEVICE_TYPE_ALL;
        final int DEVICE_INDEX = 0;
        CL.setExceptionsEnabled(true);
        int[] numPlatformsArray = new int[1];
        clGetPlatformIDs(0, null, numPlatformsArray);
        int numPlatforms = numPlatformsArray[0];
        cl_platform_id[] platforms = new cl_platform_id[numPlatforms];
        clGetPlatformIDs(platforms.length, platforms, null);
        cl_platform_id platform = platforms[PLATFORM_INDEX];
        cl_context_properties contextProperties = new cl_context_properties();
        contextProperties.addProperty(CL_CONTEXT_PLATFORM, platform);
        int numDevicesArray[] = new int[1];
        clGetDeviceIDs(platform, DEVICE_TYPE, 0, null, numDevicesArray);
        int numDevices = numDevicesArray[0];
        cl_device_id devices[] = new cl_device_id[numDevices];
        clGetDeviceIDs(platform, DEVICE_TYPE, numDevices, devices, null);
        cl_device_id device = devices[DEVICE_INDEX];
        cl_context context = clCreateContext(contextProperties, 1, new cl_device_id[]{device}, null, null, null);

        // Construction of the program
        cl_program program = clCreateProgramWithSource(context, 1, new String[]{PROGRAM_SOURCE}, null, null);

        // Construction of the queue
        cl_command_queue commandQueue = clCreateCommandQueue(context, device, 0, null);

        // Build the program
        clBuildProgram(program, 0, null, null, null, null);

        // Create the kernel
        cl_kernel kernel = clCreateKernel(program, "reconstructKernel", null);

        // Constructing pointers to array data
        Pointer ptrGX = Pointer.to(gx);
        Pointer ptrGY = Pointer.to(gy);
        Pointer ptrVal = Pointer.to(val);

        // Construction of memory objects
        cl_mem[] memObjects = new cl_mem[3];
        memObjects[0] = clCreateBuffer(
                context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                Sizeof.cl_float * gx.length,
                ptrGX,
                null
        );
        memObjects[1] = clCreateBuffer(
                context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                Sizeof.cl_float * gy.length,
                ptrGY,
                null
        );
        memObjects[2] = clCreateBuffer(
                context, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR,
                Sizeof.cl_float * val.length,
                ptrVal,
                null
        );

        // Send the parameters to the kernel
        clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memObjects[0]));
        clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memObjects[1]));
        clSetKernelArg(kernel, 2, Sizeof.cl_mem, Pointer.to(memObjects[2]));
        clSetKernelArg(kernel, 3, Sizeof.cl_int, Pointer.to(new int[]{width}));
        clSetKernelArg(kernel, 4, Sizeof.cl_int, Pointer.to(new int[]{height}));

        // Prepare work infos
        long[] global_work_size_01 = new long[]{height - 3};
        long[] global_work_size_23 = new long[]{width - 3};
        long[] local_work_size = new long[]{1};

        // Run the many iterations
        long initTime = System.currentTimeMillis() - chrono;
        long start = System.currentTimeMillis();
        for (int i = 0; i < nbSteps; i++) {
            int step = (i % 4);
            long[] global_work_size = (step%2==0) ? global_work_size_01 : global_work_size_23;

            // Tell the kernel which step has to be executed
            clSetKernelArg(kernel, 5, Sizeof.cl_int, Pointer.to(new int[]{step}));

            // Prepare the event which will have to be waited for
            cl_event lock = new cl_event();

            // Starts the kernel
            clEnqueueNDRangeKernel(
                    commandQueue,
                    kernel,
                    1,
                    null,
                    global_work_size,
                    local_work_size,
                    0,
                    null,
                    lock
            );
        }

        // Get the output
        clEnqueueReadBuffer(
                commandQueue,
                memObjects[2],
                CL_TRUE,
                0,
                val.length * Sizeof.cl_float,
                ptrVal,
                0,
                null,
                null
        );

        // Release the stuff
        clReleaseMemObject(memObjects[0]);
        clReleaseMemObject(memObjects[1]);
        clReleaseMemObject(memObjects[2]);
        clReleaseKernel(kernel);
        clReleaseProgram(program);
        clReleaseCommandQueue(commandQueue);
        clReleaseContext(context);

        setValues1D(val);
        long compTime = System.currentTimeMillis() - chrono;
        
        String platformName = getString(platforms[currentGPU], CL_PLATFORM_NAME);
        
        long workMem = width*height*4;
        long nbOp    = nbSteps * width;
        long taskMem = (width+height)/2*4;
        System.out.println("result;"+platformName+";"+workMem+";"+taskMem+";"+nbOp+";"+initTime+";"+compTime);
    }
    
    private static String getString(cl_platform_id platform, int paramName)
    {
        // Obtain the length of the string that will be queried
        long size[] = new long[1];
        clGetPlatformInfo(platform, paramName, 0, null, size);

        // Create a buffer of the appropriate size and fill it with the info
        byte buffer[] = new byte[(int)size[0]];
        clGetPlatformInfo(platform, paramName, buffer.length, Pointer.to(buffer), null);

        // Create a string from the buffer (excluding the trailing \0 byte)
        return new String(buffer, 0, buffer.length-1);
    }

    private static final String PROGRAM_SOURCE
            = "__kernel void "
            + "reconstructKernel(__global const float *gx,"
            + "                  __global const float *gy,"
            + "                  __global       float *val,"
            + "                  int width,"
            + "             int height,"
            + "             int step)"
            + "{"
            + "    int gid = get_global_id(0)+1;"
            + "    switch (step) {"
            + "        case 0:"
            + "            for (int x=1; x<width-2; x++) {"
            + "                unsigned int p = gid*width+x;"
            + "                val[p] = (val[p] + val[p-1] + gx[p-1]) / 2;"
            + "            }"
            + "            break;"
            + "        case 2:"
            + "            for (int x=width-2; x>=1; x--) {"
            + "                unsigned int p = gid*width + x;"
            + "                val[p] = (val[p] + val[p+1] - gx[p]) / 2;"
            + "            }"
            + "            break;"
            + "        case 1:"
            + "            for (int y=1; y<height-2; y++) {"
            + "                unsigned int p = y*width + gid;"
            + "                val[p] = (val[p] + val[p-width] + gy[p-width]) / 2;"
            + "            }"
            + "            break;"
            + "        case 3:"
            + "            for (int y=height-2; y>=1; y--) {"
            + "                unsigned int p = y*width + gid;"
            + "                val[p] = (val[p] + val[p+width] - gy[p]) / 2;"
            + "            }"
            + "            break;"
            + "    }"
            + "}";

    public void multiplyGradient(Image img, GradientMap target, int x, int y, float boost) {
        for (int dx = 0; dx < width; dx++) {
            int px = x + dx;
            if (px >= target.width - 2 || px<1) {
                continue;
            }
            for (int dy = 0; dy < height; dy++) {
                int py = y + dy;
                if (py >= target.height - 2 || py<1) {
                    continue;
                }
                if (img.getValues(px, py)[0]<0.025 && img.getValues(px, py)[1]<0.025 && img.getValues(px, py)[2]<0.025) {
                    continue;
                }
                target.gx[px][py] *= 1 - (1-val[dx][dy]) * boost;
                target.gy[px][py] *= 1 - (1-val[dx][dy]) * boost;
            }
        }
    }
    
    public float weightedOrientationDifference(GradientMap o) {
        if (o.width!=width || o.height!=height) {
            throw new IllegalArgumentException("dimensions mismatch");
        }
        
        
        float sum = 0.0f;
        for (int x=0; x<width; x++) {
            for (int y=0; y<height; y++) {
                float d = gx[x][y]*o.gy[x][y] - gy[x][y]*o.gx[x][y];
                sum += (d*d)/2;
            }
        }
        
        return sum / (width*height);
    }
}
