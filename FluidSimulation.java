import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 * FluidSimulation.java
 * ---------------------------------------------------
 * A real-time 2D fluid simulation using Jos Stam's
 * "Stable Fluids" algorithm.
 *
 * HOW TO RUN:
 *   javac FluidSimulation.java
 *   java FluidSimulation
 *
 * CONTROLS:
 *   Left-click + drag  → paint fluid
 *   Right-click + drag → draw walls
 *   C key              → clear everything
 * ---------------------------------------------------
 */
public class FluidSimulation extends JPanel implements Runnable {

    // ─── Grid Settings ────────────────────────────────
    static final int N  = 80;           // grid cells per side
    static final int SZ = (N+2)*(N+2); // total cells (includes border)

    // ─── Physics Constants ────────────────────────────
    static final float VISCOSITY = 0.000001f; // how "thick" the fluid is
    static final float DIFFUSION = 0.00003f;  // how fast it spreads
    static final float DT        = 0.15f;     // time step per frame
    static final float GRAVITY   = 0.35f;     // downward pull each frame

    // ─── Simulation Arrays ────────────────────────────
    float[] dens,     densPrev;   // density (how much fluid is here)
    float[] velX,     velXPrev;   // horizontal velocity
    float[] velY,     velYPrev;   // vertical velocity
    boolean[] wall;               // wall mask

    // ─── Display ──────────────────────────────────────
    static final int WINDOW = 640;         // window size in pixels
    BufferedImage image;
    JFrame frame;

    // ─── Mouse State ──────────────────────────────────
    int mouseX, mouseY, prevMouseX, prevMouseY;
    boolean leftDown, rightDown;

    // ══════════════════════════════════════════════════
    //  ENTRY POINT
    // ══════════════════════════════════════════════════
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FluidSimulation sim = new FluidSimulation();
            sim.start();
        });
    }

    // ══════════════════════════════════════════════════
    //  CONSTRUCTOR — initialise everything
    // ══════════════════════════════════════════════════
    public FluidSimulation() {
        dens     = new float[SZ];  densPrev = new float[SZ];
        velX     = new float[SZ];  velXPrev = new float[SZ];
        velY     = new float[SZ];  velYPrev = new float[SZ];
        wall     = new boolean[SZ];
        image    = new BufferedImage(N+2, N+2, BufferedImage.TYPE_INT_RGB);

        setPreferredSize(new Dimension(WINDOW, WINDOW));
        setBackground(Color.BLACK);

        // ── Mouse listeners ──────────────────────────
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e)  { updateMouse(e); }
            public void mouseReleased(MouseEvent e) {
                leftDown  = false;
                rightDown = false;
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) { updateMouse(e); }
        });

        // ── Keyboard listener ─────────────────────────
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyChar() == 'c' || e.getKeyChar() == 'C') clearAll();
            }
        });
    }

    void updateMouse(MouseEvent e) {
        prevMouseX = mouseX;
        prevMouseY = mouseY;
        mouseX     = e.getX();
        mouseY     = e.getY();
        leftDown   = SwingUtilities.isLeftMouseButton(e);
        rightDown  = SwingUtilities.isRightMouseButton(e);
    }

    void clearAll() {
        Arrays.fill(dens, 0);  Arrays.fill(densPrev, 0);
        Arrays.fill(velX, 0);  Arrays.fill(velXPrev, 0);
        Arrays.fill(velY, 0);  Arrays.fill(velYPrev, 0);
        Arrays.fill(wall, false);
    }

    // ══════════════════════════════════════════════════
    //  WINDOW SETUP & GAME LOOP
    // ══════════════════════════════════════════════════
    void start() {
        frame = new JFrame("💧 Fluid Simulation — drag to paint, C to clear");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(this);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        new Thread(this).start();
    }

    @Override
    public void run() {
        // Target ~60 frames per second
        long targetFrameTime = 1000 / 60;
        while (true) {
            long start = System.currentTimeMillis();
            update();
            repaint();
            long elapsed = System.currentTimeMillis() - start;
            long sleep   = targetFrameTime - elapsed;
            if (sleep > 0) {
                try { Thread.sleep(sleep); } catch (InterruptedException ignored) {}
            }
        }
    }

    // ══════════════════════════════════════════════════
    //  MAIN UPDATE — called every frame
    // ══════════════════════════════════════════════════
    void update() {
        // 1. Clear previous-frame sources
        Arrays.fill(densPrev, 0);
        Arrays.fill(velXPrev, 0);
        Arrays.fill(velYPrev, 0);

        // 2. Apply user input
        applyMouseInput();

        // 3. Run physics
        velocityStep();
        densityStep();
    }

    // ══════════════════════════════════════════════════
    //  USER INPUT → paint fluid or walls
    // ══════════════════════════════════════════════════
    void applyMouseInput() {
        if (!leftDown && !rightDown) return;

        // Convert pixel coords to grid coords
        int gx  = (int)((mouseX     / (float) WINDOW) * N) + 1;
        int gy  = (int)((mouseY     / (float) WINDOW) * N) + 1;
        int pgx = (int)((prevMouseX / (float) WINDOW) * N) + 1;
        int pgy = (int)((prevMouseY / (float) WINDOW) * N) + 1;

        // Paint a small brush (3x3 area)
        for (int di = -2; di <= 2; di++) {
            for (int dj = -2; dj <= 2; dj++) {
                int ci = gx + di;
                int cj = gy + dj;
                if (ci < 1 || ci > N || cj < 1 || cj > N) continue;

                if (leftDown) {
                    // Add fluid density and give it velocity from mouse motion
                    densPrev[ix(ci, cj)] += 200f;
                    velXPrev[ix(ci, cj)] += (gx - pgx) * 2.5f;
                    velYPrev[ix(ci, cj)] += (gy - pgy) * 2.5f;
                    wall[ix(ci, cj)]      = false;

                } else if (rightDown) {
                    // Place a solid wall
                    wall[ix(ci, cj)]  = true;
                    dens[ix(ci, cj)]  = 0;
                    velX[ix(ci, cj)]  = 0;
                    velY[ix(ci, cj)]  = 0;
                }
            }
        }
    }

    // ══════════════════════════════════════════════════
    //  PHYSICS — VELOCITY STEP
    //  Updates where and how fast fluid is moving
    // ══════════════════════════════════════════════════
    void velocityStep() {
        // Add any velocity sources (from user input)
        addSource(velX, velXPrev);
        addSource(velY, velYPrev);

        // Apply gravity — pull fluid downward
        for (int j = 1; j <= N; j++)
            for (int i = 1; i <= N; i++)
                velY[ix(i, j)] += DT * GRAVITY;

        // Diffuse velocity (viscosity makes nearby cells affect each other)
        float[] tmp;
        tmp = velXPrev; velXPrev = velX; velX = tmp;
        diffuse(1, velX, velXPrev, VISCOSITY);

        tmp = velYPrev; velYPrev = velY; velY = tmp;
        diffuse(2, velY, velYPrev, VISCOSITY);

        // Project: enforce that fluid doesn't pile up (incompressibility)
        project(velX, velY, velXPrev, velYPrev);

        // Advect velocity: the velocity field carries itself
        tmp = velXPrev; velXPrev = velX; velX = tmp;
        tmp = velYPrev; velYPrev = velY; velY = tmp;
        advect(1, velX, velXPrev, velXPrev, velYPrev);
        advect(2, velY, velYPrev, velXPrev, velYPrev);

        // Project again after advection
        project(velX, velY, velXPrev, velYPrev);
    }

    // ══════════════════════════════════════════════════
    //  PHYSICS — DENSITY STEP
    //  Updates how much fluid is at each cell
    // ══════════════════════════════════════════════════
    void densityStep() {
        addSource(dens, densPrev);

        float[] tmp;
        tmp = densPrev; densPrev = dens; dens = tmp;

        // STEP 1 — Diffuse: fluid spreads to neighbours (like ink in water)
        diffuse(0, dens, densPrev, DIFFUSION);

        tmp = densPrev; densPrev = dens; dens = tmp;

        // STEP 2 — Advect: fluid is carried along by the velocity field
        advect(0, dens, densPrev, velX, velY);
    }

    // ══════════════════════════════════════════════════
    //  CORE ALGORITHM 1: addSource
    //  Simply adds a "source" value to the current array.
    //  Used to inject user input into the simulation.
    // ══════════════════════════════════════════════════
    void addSource(float[] x, float[] s) {
        for (int i = 0; i < SZ; i++)
            x[i] += DT * s[i];
    }

    // ══════════════════════════════════════════════════
    //  CORE ALGORITHM 2: diffuse
    //  Each cell blends with its 4 neighbours.
    //  Controls spreading (viscosity / diffusion).
    //
    //  b=0 → density, b=1 → x-velocity, b=2 → y-velocity
    // ══════════════════════════════════════════════════
    void diffuse(int b, float[] x, float[] x0, float diff) {
        float a = DT * diff * N * N;

        // Iterative solver — repeat 10x to converge
        for (int k = 0; k < 10; k++) {
            for (int j = 1; j <= N; j++) {
                for (int i = 1; i <= N; i++) {
                    if (wall[ix(i, j)]) continue;
                    x[ix(i,j)] = (x0[ix(i,j)] + a * (
                            x[ix(i-1, j)] +
                            x[ix(i+1, j)] +
                            x[ix(i, j-1)] +
                            x[ix(i, j+1)]
                    )) / (1 + 4 * a);
                }
            }
            setBounds(b, x);
        }
    }

    // ══════════════════════════════════════════════════
    //  CORE ALGORITHM 3: advect
    //  Moves fluid along the velocity field.
    //  Instead of pushing forward, we trace BACKWARDS:
    //    "Where did this cell's fluid come from?"
    //  Then we interpolate from that source position.
    // ══════════════════════════════════════════════════
    void advect(int b, float[] d, float[] d0, float[] u, float[] v) {
        float dt0 = DT * N; // scale dt to grid units

        for (int j = 1; j <= N; j++) {
            for (int i = 1; i <= N; i++) {
                if (wall[ix(i, j)]) continue;

                // Trace backwards along velocity
                float px = i - dt0 * u[ix(i, j)];
                float py = j - dt0 * v[ix(i, j)];

                // Clamp to grid boundaries
                px = Math.max(0.5f, Math.min(N + 0.5f, px));
                py = Math.max(0.5f, Math.min(N + 0.5f, py));

                // Bilinear interpolation from 4 surrounding cells
                int i0 = (int) px, i1 = i0 + 1;
                int j0 = (int) py, j1 = j0 + 1;
                float s1 = px - i0, s0 = 1 - s1;
                float t1 = py - j0, t0 = 1 - t1;

                d[ix(i,j)] =
                    s0 * (t0 * d0[ix(i0, j0)] + t1 * d0[ix(i0, j1)]) +
                    s1 * (t0 * d0[ix(i1, j0)] + t1 * d0[ix(i1, j1)]);
            }
        }
        setBounds(b, d);
    }

    // ══════════════════════════════════════════════════
    //  CORE ALGORITHM 4: project
    //  Enforces incompressibility — real fluids can't
    //  pile up or vanish. This step fixes the velocity
    //  field so mass is conserved every frame.
    // ══════════════════════════════════════════════════
    void project(float[] u, float[] v, float[] p, float[] div) {
        float h = 1.0f / N;

        // Calculate divergence (how much fluid is "converging" at each cell)
        for (int j = 1; j <= N; j++) {
            for (int i = 1; i <= N; i++) {
                div[ix(i,j)] = -0.5f * h * (
                        u[ix(i+1, j)] - u[ix(i-1, j)] +
                        v[ix(i, j+1)] - v[ix(i, j-1)]
                );
                p[ix(i,j)] = 0;
            }
        }
        setBounds(0, div);
        setBounds(0, p);

        // Solve for pressure using iterative relaxation
        for (int k = 0; k < 10; k++) {
            for (int j = 1; j <= N; j++) {
                for (int i = 1; i <= N; i++) {
                    p[ix(i,j)] = (div[ix(i,j)] +
                            p[ix(i-1,j)] + p[ix(i+1,j)] +
                            p[ix(i,j-1)] + p[ix(i,j+1)]
                    ) / 4;
                }
            }
            setBounds(0, p);
        }

        // Subtract pressure gradient from velocity (corrects the flow)
        for (int j = 1; j <= N; j++) {
            for (int i = 1; i <= N; i++) {
                u[ix(i,j)] -= 0.5f * (p[ix(i+1,j)] - p[ix(i-1,j)]) / h;
                v[ix(i,j)] -= 0.5f * (p[ix(i,j+1)] - p[ix(i,j-1)]) / h;
            }
        }
        setBounds(1, u);
        setBounds(2, v);
    }

    // ══════════════════════════════════════════════════
    //  BOUNDARY CONDITIONS
    //  Handles edges of the grid + wall cells.
    //  b=1: flip x-velocity at left/right walls
    //  b=2: flip y-velocity at top/bottom walls
    //  b=0: copy (density doesn't bounce)
    // ══════════════════════════════════════════════════
    void setBounds(int b, float[] x) {
        for (int i = 1; i <= N; i++) {
            x[ix(0,   i)] = (b == 1) ? -x[ix(1, i)] : x[ix(1, i)];
            x[ix(N+1, i)] = (b == 1) ? -x[ix(N, i)] : x[ix(N, i)];
            x[ix(i,   0)] = (b == 2) ? -x[ix(i, 1)] : x[ix(i, 1)];
            x[ix(i, N+1)] = (b == 2) ? -x[ix(i, N)] : x[ix(i, N)];
        }
        // Corners: average of two neighbours
        x[ix(0,   0  )] = 0.5f * (x[ix(1,0)]   + x[ix(0,1)]);
        x[ix(0,   N+1)] = 0.5f * (x[ix(1,N+1)] + x[ix(0,N)]);
        x[ix(N+1, 0  )] = 0.5f * (x[ix(N,0)]   + x[ix(N+1,1)]);
        x[ix(N+1, N+1)] = 0.5f * (x[ix(N,N+1)] + x[ix(N+1,N)]);

        // Wall cells: zero out (fluid can't pass through)
        for (int j = 1; j <= N; j++)
            for (int i = 1; i <= N; i++)
                if (wall[ix(i, j)]) x[ix(i, j)] = 0;
    }

    // ══════════════════════════════════════════════════
    //  RENDERING — draw the density grid to screen
    // ══════════════════════════════════════════════════
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Write each cell's density as a color into a BufferedImage
        for (int j = 0; j <= N+1; j++) {
            for (int i = 0; i <= N+1; i++) {
                int color;
                if (wall[ix(i, j)]) {
                    color = 0x445566; // dark blue-grey for walls
                } else {
                    float d = Math.min(dens[ix(i, j)], 1.0f);
                    color = densityToColor(d);
                }
                image.setRGB(i, j, color);
            }
        }

        // Scale the small grid image up to fill the window
        g.drawImage(image, 0, 0, WINDOW, WINDOW, null);

        // Draw UI hint
        g.setColor(new Color(255, 255, 255, 150));
        g.setFont(new Font("Monospaced", Font.PLAIN, 12));
        g.drawString("Left-drag: fluid  |  Right-drag: wall  |  C: clear", 10, 20);
    }

    // Map a density value (0–1) to a colour:
    // black → deep blue → cyan → white → orange → red
    int densityToColor(float d) {
        if (d < 0.001f) return 0x0D1117; // near-black background
        int[][] stops = {
            {13, 17, 23},    // background black
            {13, 71, 161},   // deep blue
            {21, 101, 192},  // blue
            {66, 165, 245},  // light blue
            {128, 216, 255}, // cyan
            {255, 255, 255}, // white (peak density)
            {255, 204, 128}, // light orange
            {255, 112, 67},  // orange
            {183, 28, 28}    // dark red
        };
        float t = d * (stops.length - 1);
        int   i = Math.min((int) t, stops.length - 2);
        float f = t - i;
        int r = (int)(stops[i][0] + f * (stops[i+1][0] - stops[i][0]));
        int g = (int)(stops[i][1] + f * (stops[i+1][1] - stops[i][1]));
        int b = (int)(stops[i][2] + f * (stops[i+1][2] - stops[i][2]));
        return (r << 16) | (g << 8) | b;
    }

    // ══════════════════════════════════════════════════
    //  UTILITY — convert 2D grid coords to 1D index
    // ══════════════════════════════════════════════════
    int ix(int x, int y) {
        return x + (N + 2) * y;
    }
}