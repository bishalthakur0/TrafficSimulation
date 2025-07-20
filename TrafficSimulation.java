import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

public class TrafficSimulation extends JPanel implements ActionListener {
    // Constants
    private static final int WIDTH = 1250;
    private static final int HEIGHT = 700;
    private static final int ROAD_WIDTH = 110;
    private static final int HOR_ROAD_GAP = 150;
    private static final int VER_ROAD_GAP = 100;
    private static final int HOR_GAP = 100;
    private static final int VER_GAP = 100;
    private static final int N = 3; // No. of rows
    private static final int M = 4; // No. of columns
    private static final int NO_OF_VEHICLES = 14;

    // Data structures
    private double[][] dist = new double[5][5];
    private boolean[][] used = new boolean[5][5];
    private int[] dx = { -1, 1, 0, 0 };
    private int[] dy = { 0, 0, -1, 1 };

    private boolean pause = false;
    private boolean drawPath = false;

    private Stack<Pair<Integer, Integer>> path = new Stack<>();
    private List<Double> speeds = new ArrayList<>();
    private List<Pair<Double, Double>> positions = new ArrayList<>();
    private List<Boolean> directions = new ArrayList<>();
    private List<Character> orientations = new ArrayList<>();

    private Queue<Double> horLeft = new LinkedList<>();
    private Queue<Double> horRight = new LinkedList<>();
    private Queue<Double> verUp = new LinkedList<>();
    private Queue<Double> verDown = new LinkedList<>();

    private Map<Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>, Double> weights = new HashMap<>();
    private Map<Pair<Integer, Integer>, Pair<Integer, Integer>> parent = new HashMap<>();
    private Pair<Integer, Integer> start = new Pair<>(1, 1);
    private Pair<Integer, Integer> dest = new Pair<>(M, N);
    private Map<Pair<Integer, Integer>, Pair<Double, Double>> pointMap = new HashMap<>();
    private Map<Pair<Integer, Integer>, Double> mapSpeed = new HashMap<>();

    private double[][] colors = new double[20][3];

    // Vehicle class
    class Vehicle {
        double speedX;
        double speedY;
        double posX;
        double posY;
        char orientation;
        boolean direction;

        Vehicle(double speedX, double speedY, double posX, double posY,
                char orientation, boolean direction) {
            this.speedX = speedX;
            this.speedY = speedY;
            this.posX = posX;
            this.posY = posY;
            this.orientation = orientation;
            this.direction = direction;
        }
    }

    private List<Vehicle> vehicles = new ArrayList<>();
    private double carSpeedX = 0;
    private double carSpeedY = 0;
    private double carPosX = 0;
    private double carPosY = 0;

    private javax.swing.Timer timer; // Explicitly use Swing Timer

    public TrafficSimulation() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(0, 1, 0));

        initializeSimulation();

        // Set up timer for animation
        timer = new javax.swing.Timer(16, this); // ~60 FPS
        timer.start();

        // Add keyboard listeners
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    System.exit(0);
                } else if (e.getKeyChar() == 'p' || e.getKeyChar() == 'P') {
                    pause = true;
                } else if (e.getKeyChar() == 'r' || e.getKeyChar() == 'R') {
                    pause = false;
                }
            }
        });
    }

    private void initializeSimulation() {
        speedShuffle();
        shuffleDirections();
        colorShuffle();
        assignPositions();
        pointMap();
        findWeights();
        dijkstra();
    }

    private void speedShuffle() {
        double d = 0.005;
        for (int i = 1; i <= NO_OF_VEHICLES; i++) {
            speeds.add(d);
            d += 0.005;
        }
        Collections.shuffle(speeds);
    }

    private void shuffleDirections() {
        Random rand = new Random();
        for (int i = 1; i <= NO_OF_VEHICLES; i++) {
            if (i <= 8) {
                orientations.add('V');
                directions.add(i % 2 != 0);
            } else {
                orientations.add('H');
                directions.add(i % 2 != 0);
            }
        }
    }

    private void colorShuffle() {
        Random rand = new Random();
        for (int i = 0; i < NO_OF_VEHICLES; i++) {
            colors[i][0] = (rand.nextInt(10) * 1.0) / 10;
            colors[i][1] = (rand.nextInt(10) * 1.0) / 10;
            colors[i][2] = (rand.nextInt(10) * 1.0) / 10;
        }
    }

    private void assignPositions() {
        Random rand = new Random();

        horLeft.add(VER_GAP + 0.5 * ROAD_WIDTH - 10.0);
        horLeft.add(VER_GAP + 1.5 * ROAD_WIDTH + VER_ROAD_GAP - 10.0);
        horLeft.add(VER_GAP + 2.5 * ROAD_WIDTH + 2 * VER_ROAD_GAP - 10.0);

        horRight.add(VER_GAP + 0.5 * ROAD_WIDTH + 10.0);
        horRight.add(VER_GAP + 1.5 * ROAD_WIDTH + VER_ROAD_GAP + 10.0);
        horRight.add(VER_GAP + 2.5 * ROAD_WIDTH + 2 * VER_ROAD_GAP + 10.0);

        verUp.add(HOR_GAP + 10.0);
        verUp.add(HOR_GAP + ROAD_WIDTH + HOR_ROAD_GAP + 10.0);
        verUp.add(HOR_GAP + 2 * ROAD_WIDTH + 2 * HOR_ROAD_GAP + 10.0);
        verUp.add(HOR_GAP + 3 * ROAD_WIDTH + 3 * HOR_ROAD_GAP + 10.0);

        verDown.add(HOR_GAP + 0.5 * ROAD_WIDTH + 10.0);
        verDown.add(HOR_GAP + 1.5 * ROAD_WIDTH + HOR_ROAD_GAP + 10.0);
        verDown.add(HOR_GAP + 2.5 * ROAD_WIDTH + 2 * HOR_ROAD_GAP + 10.0);
        verDown.add(HOR_GAP + 3.5 * ROAD_WIDTH + 3 * HOR_ROAD_GAP + 10.0);

        for (int i = 0; i < NO_OF_VEHICLES; i++) {
            boolean dir = directions.get(i);
            char orient = orientations.get(i);
            double posX, posY;

            if (orient == 'H' && !dir) {
                posY = horLeft.poll();
                posX = rand.nextInt(WIDTH);
            } else if (orient == 'H' && dir) {
                posY = horRight.poll();
                posX = rand.nextInt(WIDTH);
            } else if (orient == 'V' && !dir) {
                posX = verDown.poll();
                posY = rand.nextInt(HEIGHT);
            } else { // orient == 'V' && dir
                posX = verUp.poll();
                posY = rand.nextInt(HEIGHT);
            }

            vehicles.add(new Vehicle(0, 0, posX, posY, orient, dir));
        }
    }

    private void pointMap() {
        double hor = HOR_GAP + 0.5 * ROAD_WIDTH;
        double ver;

        for (int i = 1; i <= M; i++) {
            ver = VER_GAP + 0.5 * ROAD_WIDTH;
            for (int j = 1; j <= N; j++) {
                pointMap.put(new Pair<>(i, j), new Pair<>(hor, ver));
                ver += (ROAD_WIDTH + VER_ROAD_GAP);
            }
            hor += (ROAD_WIDTH + HOR_ROAD_GAP);
        }
    }

    private void findWeights() {
        Random rand = new Random(0);
        for (int i = 1; i <= M; i++) {
            for (int j = 1; j <= N; j++) {
                weights.put(new Pair<>(new Pair<>(i, j), new Pair<>(i, j + 1)), speeds.get(rand.nextInt(14)));
                weights.put(new Pair<>(new Pair<>(i, j), new Pair<>(i, j - 1)), speeds.get(rand.nextInt(14)));
                weights.put(new Pair<>(new Pair<>(i, j), new Pair<>(i + 1, j)), speeds.get(rand.nextInt(14)));
                weights.put(new Pair<>(new Pair<>(i, j), new Pair<>(i - 1, j)), speeds.get(rand.nextInt(14)));
            }
        }
    }

    private boolean isValid(Pair<Integer, Integer> u) {
        int i = u.first, j = u.second;
        return (i >= 1 && j >= 1 && i <= M && j <= N);
    }

    private void dijkstra() {
        // Initialize distances
        for (int i = 0; i < 5; i++) {
            Arrays.fill(dist[i], Double.POSITIVE_INFINITY);
        }

        PriorityQueue<Pair<Double, Pair<Integer, Integer>>> pq = new PriorityQueue<>(
                Comparator.comparingDouble(p -> -p.first)); // Max heap

        pq.add(new Pair<>(0.0, start));
        dist[start.first][start.second] = 0;

        while (!pq.isEmpty()) {
            Pair<Double, Pair<Integer, Integer>> p = pq.poll();

            for (int i = 0; i < 4; i++) {
                int x = p.second.first + dx[i];
                int y = p.second.second + dy[i];

                if (isValid(new Pair<>(x, y))) {
                    double weight = weights.getOrDefault(
                            new Pair<>(new Pair<>(p.second.first, p.second.second), new Pair<>(x, y)), 1.0);

                    if (dist[p.second.first][p.second.second] + weight < dist[x][y]) {
                        dist[x][y] = dist[p.second.first][p.second.second] + weight;
                        pq.add(new Pair<>(dist[x][y], new Pair<>(x, y)));
                        parent.put(new Pair<>(x, y), new Pair<>(p.second.first, p.second.second));
                    }
                }
            }
        }
    }

    private void drawPath(Graphics2D g2) {
        g2.setColor(Color.RED);
        Stack<Pair<Integer, Integer>> st = new Stack<>();

        Pair<Integer, Integer> p = dest;
        path.push(p);

        while (!p.equals(start)) {
            st.push(p);
            p = parent.get(p);
        }

        p = start;

        while (!st.isEmpty()) {
            Pair<Integer, Integer> q = st.pop();
            Pair<Double, Double> pq1 = pointMap.get(p);
            Pair<Double, Double> pq2 = pointMap.get(q);

            g2.drawLine((int) Math.round(pq1.first), (int) Math.round(pq1.second),
                    (int) Math.round(pq2.first), (int) Math.round(pq2.second));

            p = q;
        }

        drawPath = true;
    }

    private void drawVehicles(Graphics2D g2) {
        for (int i = 0; i < vehicles.size(); i++) {
            Vehicle v = vehicles.get(i);
            Color color = new Color((float) colors[i][0], (float) colors[i][1], (float) colors[i][2]);
            g2.setColor(color);

            if (v.orientation == 'H' && !v.direction) {
                // Left-facing horizontal vehicle
                int[] xPoints = {
                        (int) v.posX, (int) v.posX, (int) (v.posX - 65), (int) (v.posX - 90), (int) (v.posX - 65)
                };
                int[] yPoints = {
                        (int) v.posY, (int) (v.posY - 40), (int) (v.posY - 40), (int) (v.posY - 20), (int) v.posY
                };
                g2.fillPolygon(xPoints, yPoints, 5);
            } else if (v.orientation == 'H' && v.direction) {
                // Right-facing horizontal vehicle
                int[] xPoints = {
                        (int) v.posX, (int) v.posX, (int) (v.posX + 65), (int) (v.posX + 90), (int) (v.posX + 65)
                };
                int[] yPoints = {
                        (int) v.posY, (int) (v.posY + 40), (int) (v.posY + 40), (int) (v.posY + 20), (int) v.posY
                };
                g2.fillPolygon(xPoints, yPoints, 5);
            } else if (v.orientation == 'V' && !v.direction) {
                // Down-facing vertical vehicle
                int[] xPoints = {
                        (int) v.posX, (int) (v.posX + 40), (int) (v.posX + 40), (int) (v.posX + 20), (int) v.posX
                };
                int[] yPoints = {
                        (int) v.posY, (int) v.posY, (int) (v.posY - 65), (int) (v.posY - 90), (int) (v.posY - 65)
                };
                g2.fillPolygon(xPoints, yPoints, 5);
            } else if (v.orientation == 'V' && v.direction) {
                // Up-facing vertical vehicle
                int[] xPoints = {
                        (int) v.posX, (int) v.posX, (int) (v.posX + 20), (int) (v.posX + 40), (int) (v.posX + 40)
                };
                int[] yPoints = {
                        (int) v.posY, (int) (v.posY + 65), (int) (v.posY + 90), (int) (v.posY + 65), (int) v.posY
                };
                g2.fillPolygon(xPoints, yPoints, 5);
            }
        }
    }

    private void drawRoad(Graphics2D g2) {
        // Draw horizontal roads
        g2.setColor(new Color(0.2f, 0.2f, 0.2f));
        int y = VER_GAP;
        for (int i = 1; i <= 3; i++) {
            g2.fillRect(0, y, WIDTH, ROAD_WIDTH);
            y += (VER_ROAD_GAP + ROAD_WIDTH);
        }

        // Draw vertical roads
        int x = HOR_GAP;
        for (int i = 1; i <= 4; i++) {
            g2.fillRect(x, 0, ROAD_WIDTH, HEIGHT);
            x += (HOR_ROAD_GAP + ROAD_WIDTH);
        }

        // Draw road markings
        g2.setColor(Color.WHITE);
        double[] hh = {
                VER_GAP + ROAD_WIDTH / 2.0 - 2.5,
                VER_GAP + 1.5 * ROAD_WIDTH + VER_ROAD_GAP - 2.5,
                VER_GAP + 2.5 * ROAD_WIDTH + 2 * VER_ROAD_GAP - 2.5
        };

        List<Double> vPoints = new ArrayList<>();
        vPoints.add((double) (HOR_GAP + ROAD_WIDTH));
        vPoints.add((double) (HOR_GAP + 2 * ROAD_WIDTH + HOR_ROAD_GAP));
        vPoints.add((double) (HOR_GAP + 3 * ROAD_WIDTH + 2 * HOR_ROAD_GAP));
        vPoints.add((double) (HOR_GAP + 4 * ROAD_WIDTH + 3 * HOR_ROAD_GAP));

        for (int i = 0; i < 5; i++) {
            int t = (i == 0) ? 10 : 20;
            int t1 = (i == 0) ? 75 : 100;

            for (int j = 0; j < 3; j++) {
                int xPos = (int) (vPoints.get(i) + t);
                g2.fillRect(xPos, (int) hh[j], t1, 5);
            }
        }

        double[] vv = {
                HOR_GAP + ROAD_WIDTH / 2.0 - 2.5,
                HOR_GAP + 1.5 * ROAD_WIDTH + HOR_ROAD_GAP - 2.5,
                HOR_GAP + 2.5 * ROAD_WIDTH + 2 * HOR_ROAD_GAP - 2.5,
                HOR_GAP + 3.5 * ROAD_WIDTH + 3 * HOR_ROAD_GAP - 2.5
        };

        vPoints.clear();
        vPoints.add((double) (VER_GAP + ROAD_WIDTH));
        vPoints.add((double) (VER_GAP + 2 * ROAD_WIDTH + VER_ROAD_GAP));
        vPoints.add((double) (VER_GAP + 3 * ROAD_WIDTH + 2 * VER_ROAD_GAP));

        for (int i = 0; i < 4; i++) {
            int t = (i == 0) ? 10 : 10;
            int t1 = (i == 0) ? 50 : 75;

            for (int j = 0; j < 4; j++) {
                g2.fillRect((int) vv[j], (int) (vPoints.get(i) + t), 5, t1);
            }
        }

        // Draw car
        g2.setColor(Color.RED);
        int[] carXPoints = {
                10 + (int) carSpeedX,
                10 + (int) carSpeedX,
                75 + (int) carSpeedX,
                100 + (int) carSpeedX,
                100 + (int) carSpeedX,
                75 + (int) carSpeedX
        };
        int[] carYPoints = {
                VER_GAP + (int) (0.5 * ROAD_WIDTH) + 10,
                VER_GAP + (int) (0.5 * ROAD_WIDTH) + 50,
                VER_GAP + (int) (0.5 * ROAD_WIDTH) + 50,
                VER_GAP + (int) (0.5 * ROAD_WIDTH) + 40,
                VER_GAP + (int) (0.5 * ROAD_WIDTH) + 20,
                VER_GAP + (int) (0.5 * ROAD_WIDTH) + 10
        };
        g2.fillPolygon(carXPoints, carYPoints, 6);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawRoad(g2);
        drawVehicles(g2);
        drawPath(g2);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Update car position
        carSpeedX += 1.0;
        if (carSpeedX >= WIDTH) {
            carSpeedX = 0;
        }

        // Update vehicle positions if not paused
        if (!pause) {
            for (int i = 0; i < vehicles.size(); i++) {
                Vehicle v = vehicles.get(i);
                double speed = speeds.get(i);

                if (v.orientation == 'H' && !v.direction) {
                    v.posX -= speed;
                    v.speedX = speed;
                    v.speedY = 0.0;

                    // Wrap around if off screen
                    if (v.posX < -100)
                        v.posX = WIDTH;
                } else if (v.orientation == 'H' && v.direction) {
                    v.posX += speed;
                    v.speedX = speed;
                    v.speedY = 0.0;

                    // Wrap around if off screen
                    if (v.posX > WIDTH + 100)
                        v.posX = -100;
                } else if (v.orientation == 'V' && !v.direction) {
                    v.posY -= speed;
                    v.speedX = 0.0;
                    v.speedY = speed;

                    // Wrap around if off screen
                    if (v.posY < -100)
                        v.posY = HEIGHT;
                } else if (v.orientation == 'V' && v.direction) {
                    v.posY += speed;
                    v.speedX = 0.0;
                    v.speedY = speed;

                    // Wrap around if off screen
                    if (v.posY > HEIGHT + 100)
                        v.posY = -100;
                }
            }
        }

        repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Traffic Simulation");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.add(new TrafficSimulation());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    // Simple Pair class since Java doesn't have one built-in
    static class Pair<A, B> {
        final A first;
        final B second;

        Pair(A first, B second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Pair<?, ?> pair = (Pair<?, ?>) o;
            return first.equals(pair.first) && second.equals(pair.second);
        }

        @Override
        public int hashCode() {
            return 31 * first.hashCode() + second.hashCode();
        }
    }
}