package com.antheroiot.utils;

import android.util.Log;

/**
 * Created by leo on 18/1/15
 */
public class ColorHelper {
    static final String LOG_TAG = new Throwable().getStackTrace()[0].getClassName();

    static public class ColorPair {
        public int W;
        public int Y;

        public ColorPair(int y, int w) {
            W = w;
            Y = y;
        }
    }

    static public class ColorTriplet {
        public int R;
        public int G;
        public int B;

        public ColorTriplet(int r, int g, int b) {
            R = r;
            G = g;
            B = b;
        }
    }

    static   final int  Max_YW=255;
    static   final int  MIN_YW=5;
    static   final int  FULL_W_VAL=10;
    static   final int  FULL_Y_VAL=240;



    static final int INDEX_R=1;
    static final int INDEX_G=3;
    static final int INDEX_B=5;

    static int RGB_NEW[][]=
            {
                    { 0, 0, 0, 0, 0, 0},
                    {145, 171, 640, 155, 95, 158 },
                    {290, 325, 1280, 307, 190, 323 },
                    {435, 485, 1920, 461, 285, 515 },
                    {580, 680, 2560, 608, 380, 713 },
                    {725, 816, 3200, 781, 475, 891 },
                    {870, 960, 3840, 923, 570, 1060 },
                    {1015, 1100, 4480, 1069, 665, 1233 },
                    {1160, 1290, 5120, 1213, 760, 1413 },
                    {1305, 1415, 5760, 1387, 855, 1590 },
                    {1450, 1570, 6400, 1550, 950, 1773 },
                    {1595, 1709, 7040, 1698, 1045, 1953 },
                    {1740, 1840, 7680, 1860, 1140, 2143 },
                    {1885, 2015, 8320, 2015, 1235, 2310 },
                    {2030, 2180, 8960, 2166, 1330, 2490 },
                    {2175, 2320, 9600, 2319, 1425, 2670 },
                    {2320, 2425, 10240, 2480, 1520, 2854 },
                    {2465, 2575, 10880, 2630, 1615, 3020 },
                    {2610, 2775, 11520, 2781, 1710, 3200 },
                    {2755, 2925, 12160, 2930, 1805, 3340 },
                    {2900, 3100, 12800, 3100, 1900, 3524 },
                    {3045, 3250, 13440, 3259, 1995, 3700 },
                    {3190, 3385, 14080, 3420, 2090, 3880 },
                    {3335, 3540, 14720, 3527, 2185, 4030 },
                    {3480, 3675, 15360, 3665, 2280, 4208 },
                    {3625, 3815, 16000, 3812, 2375, 4390 },
                    {3770, 3950, 16640, 3984, 2470, 4533 },
                    {3915, 4150, 17280, 4128, 2565, 4733 },
                    {4060, 4340, 17920, 4275, 2660, 4923 },
                    {4205, 4475, 18560, 4431, 2755, 5115 },
                    {4350, 4625, 19200, 4586, 2850, 5300 },
                    {4495, 4765, 19840, 4730, 2945, 5485 },
                    {4640, 4920, 20480, 4878, 3040, 5667 },
                    {4785, 5105, 21120, 5051, 3135, 5805 },
                    {4930, 5290, 21760, 5208, 3230, 5865 },
                    {5075, 5468, 22400, 5362, 3325, 5975 },
                    {5220, 5629, 23040, 5520, 3420, 6051 },
                    {5365, 5778, 23680, 5668, 3515, 6145 },
                    {5510, 5939, 24320, 5803, 3610, 6255 },
                    {5655, 6075, 24960, 5947, 3705, 6361 },
                    {5800, 6275, 25600, 6163, 3800, 6475 },
                    {5945, 6420, 26240, 6313, 3895, 6735 },
                    {6090, 6581, 26880, 6481, 3990, 6945 },
                    {6235, 6718, 27520, 6653, 4085, 7045 },
                    {6380, 7051, 28160, 6812, 4180, 7215 },
                    {6525, 7199, 28800, 6994, 4275, 7332 },
                    {6670, 7352, 29440, 7168, 4370, 7433 },
                    {6815, 7476, 30080, 7376, 4465, 7659 },
                    {6960, 7611, 30720, 7550, 4560, 7655 },
                    {7105, 7748, 31360, 7730, 4655, 7763 },
                    {7250, 7900, 32000, 7911, 4750, 7892 },
                    {7395, 8192, 32640, 8192, 4845, 8192 },
                    {7395, 8192, 32640, 8192, 4845, 8192 },
            };

    static int RGB[][]=RGB_NEW;

    private static int gammaCorrect(int level, float newGamma) {
        return (int) (255 * (Math.pow((double) level / (double) 255, newGamma)));
    }


    static final float DEFAULT_GAMMA=3.0f;

    public static ColorTriplet translateColor(int r, int g, int b) {
        return translateColorLinear(gammaCorrect(r, DEFAULT_GAMMA), gammaCorrect(g, DEFAULT_GAMMA), gammaCorrect(b, DEFAULT_GAMMA));
    }

    private static ColorTriplet translateColorLinear(int r, int g, int b)
    {
        int idx=r/5;
        int offset=r-idx*5;
        int delta=RGB[idx+1][INDEX_R]-RGB[idx][INDEX_R];
        int translated_R,translated_G,translated_B;
        translated_R=RGB[idx][INDEX_R]+offset*delta/5;

        idx=g/5;
        offset=g-idx*5;
        delta=RGB[idx+1][INDEX_G]-RGB[idx][INDEX_G];
        translated_G=RGB[idx][INDEX_G]+offset*delta/5;

        idx=b/5;
        offset=b-idx*5;
        delta=RGB[idx+1][INDEX_B]-RGB[idx][INDEX_B];
        translated_B=RGB[idx][INDEX_B]+offset*delta/5;

        translated_R=translated_R >> 5;
        if (translated_R>255)
            translated_R=255;
        translated_G=translated_G >> 5;
        if (translated_G>255)
            translated_G=255;
        translated_B=translated_B >> 5;
        if (translated_B>255)
            translated_B=255;

        return new ColorTriplet(translated_R, translated_G, translated_B);
    }


    // YW look up table for color bulb
    static int YW_Color[][][]={
            {{  0,  5 },{  0,  20 },{  0,  41 },{  0,  61 },{  0,  88 },{  0,  110 },{  0,  139 },{  0,  169 },{  0,  196 },{  0,  224 },{  0,  250 },{  0,  250 },},
            {{  5,  5 },{  5,  5 },{  5,  20 },{  6,  45 },{  7,  70 },{  11,  90 },{  13,  116 },{  14,  140 },{  17,  168 },{  20,  195 },{  21,  221 },{  21,  221 },},
            {{  5,  5 },{  8,  8 },{  15,  15 },{  25,  24 },{  42,  40 },{  57,  53 },{  71,  68 },{  84,  80 },{  98,  92 },{  114,  109 },{  126,  121 },{  126,  121 },},
            {{  5,  5 },{  5,  5 },{  25,  6 },{  45,  10 },{  62,  13 },{  92,  17 },{  110,  20 },{  135,  23 },{  153,  27 },{  175,  31 },{  205,  36 },{  205,  36 },},
            {{  5,  0 },{  21,  0 },{  38,  0 },{  66,  0 },{  90,  0 },{  114,  0 },{  143,  0 },{  170,  0 },{  202,  0 },{  233,  0 },{  250,  0 },{  250,  0 },},
            {{  0,  0 },{  0,  0 },{  0,  0 },{  0,  0 },{  0,  0 },{  0,  0 },{  0,  0 },{  0,  0 },{  0,  0 },{  0,  0 },{  0,  0 },{  0,  0 },}
    };

    static public ColorPair translateYWLevelsColor(int Y_val, int W_val) {
        int w_offset, y_offset;

        int idx_Y, idx_W, delta_Y, delta_W;

        if (W_val <= FULL_W_VAL) {
            //: 2700K
            y_offset = 0;
            if (Y_val < MIN_YW) Y_val = MIN_YW;
            w_offset = (Y_val);
        } else if (W_val >= FULL_Y_VAL) {
            //: 6000K
            if (Y_val < MIN_YW) Y_val = MIN_YW;
            y_offset = (Y_val);
            w_offset = 0;
        } else {
            idx_W = (W_val * 4) / 250;
            delta_W = W_val - idx_W * 250 / 4;

            idx_Y = (Y_val * 10) / 250;
            delta_Y = Y_val - idx_Y * 25;

            //: Y=delta_Y_over_W*%W+delta_Y_over_Y*%Y
            int delta_Y0, w0_over_Y, y0_over_Y, delta_Y1, w1_over_Y, y1_over_Y, delta_W0, delta_W1;

            delta_W0 = YW_Color[idx_W][idx_Y + 1][1] - YW_Color[idx_W][idx_Y][1];
            w0_over_Y = YW_Color[idx_W][idx_Y][1] + delta_W0 * delta_Y * 10 / 250;
            delta_W1 = YW_Color[idx_W + 1][idx_Y + 1][1] - YW_Color[idx_W + 1][idx_Y][1];
            w1_over_Y = YW_Color[idx_W + 1][idx_Y][1] + delta_W1 * delta_Y * 10 / 250;
            w_offset = w0_over_Y + (w1_over_Y - w0_over_Y) * delta_W * 4 / 250;

            delta_Y0 = YW_Color[idx_W][idx_Y + 1][0] - YW_Color[idx_W][idx_Y][0];
            y0_over_Y = YW_Color[idx_W][idx_Y][0] + delta_Y0 * delta_Y * 10 / 250;
            delta_Y1 = YW_Color[idx_W + 1][idx_Y + 1][0] - YW_Color[idx_W + 1][idx_Y][0];
            y1_over_Y = YW_Color[idx_W + 1][idx_Y][0] + delta_Y1 * delta_Y * 10 / 250;
            y_offset = y0_over_Y + (y1_over_Y - y0_over_Y) * delta_W * 4 / 250;
            if (y_offset > Max_YW) y_offset = Max_YW;
            if (y_offset < MIN_YW) y_offset = MIN_YW;
            if (w_offset > Max_YW) w_offset = Max_YW;
            if (w_offset < MIN_YW) w_offset = MIN_YW;
        }

        ColorPair result = new ColorPair(0, 0);
        result.Y = (y_offset);
        result.W = (w_offset);

        Log.d(LOG_TAG, String.format("converted (%d, %d) to (%d, %d)", W_val, Y_val, result.W, result.Y));

        return result;

    }

}