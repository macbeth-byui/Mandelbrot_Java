package macbeth;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class Mandelbrot extends JPanel {
    private static final int FRACTAL_ITERATIONS = 255;
    private static final double FRACTAL_ESCAPE = 2;
    private static final int WORKER_THREADS = 10;
    private static final int WINDOW_HEIGHT = 800;
    private static final int WINDOW_WIDTH = 800;
    private static final double INIT_VIRTUAL_GRID_XMIN = -2.0;
    private static final double INIT_VIRTUAL_GRID_XMAX = 2.0;
    private static final double INIT_VIRTUAL_GRID_YMIN = -2.0;
    private static final double INIT_VIRTUAL_GRID_YMAX = 2.0;

    private double xmin;
    private double xmax;
    private double ymin;
    private double ymax;

    private List<PhysicalPoint> drawing;

    private class VirtualPoint {
        public double x;
        public double y;

        public VirtualPoint() {
            x = 0;
            y = 0;
        }

        public VirtualPoint(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }
    
    private class PhysicalPoint {
        public int x;
        public int y;
        public Color color;

        public PhysicalPoint() {
            x = 0;
            y = 0;
            color = new Color(0,0,0);
        }

        public PhysicalPoint(int x, int y, Color color) {
            this.x = x;
            this.y = y;
            this.color = color;
        }
    }

    public Mandelbrot() {
        xmin = INIT_VIRTUAL_GRID_XMIN;
        xmax = INIT_VIRTUAL_GRID_XMAX;
        ymin = INIT_VIRTUAL_GRID_YMIN;
        ymax = INIT_VIRTUAL_GRID_YMAX;
        drawing = new ArrayList<>();
        setBackground(Color.BLACK);
        addMouseListener(new MouseListener() {
            public void mousePressed(MouseEvent e) {}
         
             public void mouseReleased(MouseEvent e) {}
         
             public void mouseEntered(MouseEvent e) {}
         
             public void mouseExited(MouseEvent e) {}
         
             public void mouseClicked(MouseEvent e) {
                 zoom(e.getX(), e.getY(), 0.8);
                 calcMandelbrot();
             }
        });

        JFrame frame = new JFrame("Mandelbrot - Java");
        frame.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(this);
        frame.setVisible(true);
    }

    public void init() {
        calcMandelbrot();
    }

    private PhysicalPoint calcMandelbrotPoint(VirtualPoint coord) {
        double prevX = coord.x;
        double prevY = coord.y;
        int count;
        for (count = 0; count < FRACTAL_ITERATIONS; count++) {
            double x = (prevX * prevX) - (prevY * prevY) + coord.x;
            double y = (2 * (prevX * prevY)) + coord.y;
            double dist = Math.sqrt(x*x + y*y);
            prevX = x;
            prevY = y;
            if (dist > FRACTAL_ESCAPE) {
                break;
            }
        }
        if (count > 0 && count < FRACTAL_ITERATIONS) {
            PhysicalPoint calcPoint = this.new PhysicalPoint();
            calcPoint.x = (int)(((coord.x - xmin) / (xmax - xmin)) * (double)getWidth());
            calcPoint.y = (int)(((coord.y - ymin) / (ymax - ymin)) * (double)getHeight());
            calcPoint.color = new Color(Math.min(255, count *10), count, count);
            return calcPoint;            
        } 
        return null;
    }

    private void calcMandelbrotWorker(List<VirtualPoint> pointsSubset, List<PhysicalPoint> results) {
        for (VirtualPoint point : pointsSubset) {
            PhysicalPoint calcPoint = calcMandelbrotPoint(point);
            if (calcPoint != null) {
                results.add(calcPoint);
            }
        }
    }

    private void calcMandelbrot() {
        List<VirtualPoint> points = new ArrayList<>();
        double deltaX = ((xmax - xmin) / (double)getWidth());
        double deltaY = ((ymax - ymin) / (double)getHeight());
        double x = xmin;
        while (x <= xmax) {
            double y = ymin;
            while (y <= ymax) {
                points.add(this.new VirtualPoint(x, y));
                y += deltaY;
            }
            x += deltaX;
        }
        List<Thread> threads = new ArrayList<>();
        List<List<PhysicalPoint>> results = new ArrayList<>();

        for (int block=0; block<WORKER_THREADS; block++) {
            int startRange = (int)((points.size() / WORKER_THREADS)) * block;
            int endRange = (int)((points.size() / WORKER_THREADS)) * (block+1);
            List<VirtualPoint> subset = points.subList(startRange, endRange);
            List<PhysicalPoint> result = new ArrayList<>();
            Thread thread = new Thread() {
                public void run() {
                    calcMandelbrotWorker(subset, result);
                }
            };
            threads.add(thread);
            results.add(result);
            thread.start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {}
        }

        drawing.clear();
        for (List<PhysicalPoint> result : results) {
            drawing.addAll(result);
        }

        repaint();
    }

    private void zoom(int screenX, int screenY, double ratio) {
        double virtualGridXSize = ((xmax - xmin)/2.0) * ratio;
        double virtualGridYSize = ((ymax - ymin)/2.0) * ratio;
        double virtualX = (((screenX / (double)getWidth())) * (xmax - xmin)) + xmin; 
        double virtualY = (((screenY / (double)getHeight())) * (ymax - ymin)) + ymin;
        xmin = virtualX - virtualGridXSize;
        xmax = virtualX + virtualGridXSize;
        ymin = virtualY - virtualGridYSize;
        ymax = virtualY + virtualGridYSize;
    }

    public void paintComponent(Graphics context) {
        super.paintComponent(context);
        for (PhysicalPoint point : drawing) {
            context.setColor(point.color);
            context.drawLine(point.x, point.y, point.x, point.y);
        }
    }

    public static void main(String []args) {
        Mandelbrot mandelbrot = new Mandelbrot();
        mandelbrot.init();
    }



}