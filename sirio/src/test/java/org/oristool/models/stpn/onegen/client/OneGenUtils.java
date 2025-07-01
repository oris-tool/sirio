/* This program is part of the ORIS Tool.
 * Copyright (C) 2011-2021 The ORIS Authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.oristool.models.stpn.onegen.client;

import org.oristool.analyzer.state.State;
import org.oristool.models.pn.PetriStateFeature;
import org.oristool.models.stpn.TransientSolution;
import org.oristool.models.stpn.TransientSolutionViewer;
import org.oristool.models.stpn.trees.DeterministicEnablingState;
import org.oristool.models.stpn.trees.Regeneration;
import org.oristool.petrinet.Marking;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OneGenUtils {

    public static void printLengths(double[][][] regen, double[][][] oneGen) {
        System.out.println("[" + regen.length + "][" + regen[0].length + "][" + regen[0][0].length
                + "]\n[" + oneGen.length + "][" + oneGen[0].length + "][" + oneGen[0][0].length
                + "]");
    }

    public static <T> double[][][] order(
            TransientSolution<DeterministicEnablingState, Marking> solutionRegenerative,
            TransientSolution<DeterministicEnablingState, Marking> solutionOneGen,
            List<T> thirdDimensionRegen, List<T> thirdDimensionOneGen, double[][][] toBeOrdered) {

        double[][][] ordered =
                new double[toBeOrdered.length][toBeOrdered[0].length][toBeOrdered[0][0].length];
        for (int t = 0; t < toBeOrdered.length; t++) {
            for (int i = 0; i < toBeOrdered[t].length; i++) {
                if (i >= solutionRegenerative.getRegenerations().size())
                    continue;
                int indexRegen = -1;
                DeterministicEnablingState regen = solutionRegenerative.getRegenerations().get(i);
                for (int s = 0; s < solutionOneGen.getRegenerations().size(); s++) {
                    if (solutionOneGen.getRegenerations().get(s).equals(regen)) {
                        indexRegen = s;
                        break;
                    }
                }
                for (int j = 0; j < toBeOrdered[t][i].length; j++) {
                    if (j >= thirdDimensionRegen.size())
                        continue;
                    int indexState = -1;
                    T state = thirdDimensionRegen.get(j);
                    for (int s = 0; s < thirdDimensionOneGen.size(); s++) {
                        if (thirdDimensionOneGen.get(s).equals(state)) {
                            indexState = s;
                            break;
                        }
                    }
                    ordered[t][i][j] = toBeOrdered[t][indexRegen][indexState];
                }
            }
        }
        return ordered;
    }

    public static List<String> getRegenerationsOrMarkings(List<State> states) {
        List<String> labels = new ArrayList<String>();
        states.forEach((state) -> labels.add(state.hasFeature(Regeneration.class)
                ? state.getFeature(Regeneration.class).getValue().toString()
                : state.getFeature(PetriStateFeature.class).getMarking().toString()));
        return labels;
    }

    private static String padRight(String s, int n) {
        return String.format("%1$-" + n + "s", s);
    }

    public static void printMatrix(String title, int pad, List<String> secondDimension,
            List<String> thirdDimension, double[][][] matrix) {
        String delim = "-";
        System.out.println("### " + title + " ###");
        for (int t = 0; t < matrix.length; t++) {
            for (String state : thirdDimension) {
                System.out.print(padRight(state.toString(), pad));
            }
            System.out.println(
                    "\n" + new String(new char[pad * matrix[t][0].length]).replace("\0", delim)
                            + " t=" + t);
            for (int i = 0; i < matrix[t].length; i++) {
                for (int j = 0; j < matrix[t][i].length; j++) {
                    System.out.print(padRight(String.format("%.5f  ", matrix[t][i][j]), pad));
                }
                System.out.println("|  " + secondDimension.get(i));
            }
            System.out.println(new String(new char[pad * matrix[t][matrix[t].length - 1].length])
                    .replace("\0", delim));
        }
        System.out.println();
    }

    public static void printCompare(
            TransientSolution<DeterministicEnablingState, Marking> solutionRegenerative,
            double[][][] regen, double[][][] onegen) {
        System.out.print("REGEN:      ");
        for (int i = 0; i < regen[0][0].length - 1; i++) {
            System.out.print("         ");
        }
        System.out.println("ONEGEN:");
        for (int t = 0; t < regen.length; t++) {
            for (int k = 0; k < regen[0][0].length; k++) {
                System.out.print("------------------");
            }
            System.out.println("- t=" + t);
            for (int i = 0; i < regen[t].length; i++) {
                for (int k = 0; k < regen[t][i].length; k++) {
                    System.out.print(String.format("%.5f", regen[t][i][k]) + "  ");
                }
                System.out.print("|");
                for (int j = 0; j < regen[t][i].length; j++) {
                    System.out.print("  " + String.format("%.5f", onegen[t][i][j]));
                }
                System.out.println(
                        "|  " + solutionRegenerative.getRegenerations().get(i).getMarking());
            }
            for (int k = 0; k < regen[t][0].length; k++) {
                System.out.print("------------------");
            }
            System.out.println("-");
        }
        System.out.println();
    }

    public static void testMatrices(double[][][] regen, double[][][] onegen) {
        for (int t = 0; t < regen.length; t++) {
            for (int i = 0; i < regen[t].length; i++) {
                for (int k = 0; k < regen[t][i].length; k++) {
                    assertEquals(regen[t][i][k], onegen[t][i][k], 0.0001);
                }
            }
        }
    }

    public static void printAndTest(
            TransientSolution<DeterministicEnablingState, Marking> solutionRegenerative,
            double[][][] regen, double[][][] onegen) {
        printCompare(solutionRegenerative, regen, onegen);
        testMatrices(regen, onegen);
    }

    public static void testLengths(
            TransientSolution<DeterministicEnablingState, Marking> solutionRegenerative,
            TransientSolution<DeterministicEnablingState, Marking> solutionOneGen) {
        assertEquals(solutionOneGen.getSolution().length,
                solutionRegenerative.getSolution().length);
        assertEquals(solutionOneGen.getSolution()[0].length,
                solutionRegenerative.getSolution()[0].length);
        assertEquals(solutionOneGen.getSolution()[0][0].length,
                solutionRegenerative.getSolution()[0][0].length);
    }

    public static void testRegenerations(
            TransientSolution<DeterministicEnablingState, Marking> solutionRegenerative,
            TransientSolution<DeterministicEnablingState, Marking> solutionOneGen) {
        List<DeterministicEnablingState> regenerationsRegen = solutionRegenerative
                .getRegenerations();
        List<DeterministicEnablingState> regenerationsOneGen = solutionOneGen.getRegenerations();

        DeterministicEnablingState found = null;
        for (DeterministicEnablingState regenerationRegen : regenerationsRegen) {
            System.out.println("Regeneration " + regenerationRegen.toString());
            for (DeterministicEnablingState regenerationOneGen : regenerationsOneGen) {
                if (regenerationRegen.equals(regenerationOneGen)) {
                    found = regenerationOneGen;
                    break;
                }
            }
            assertTrue(found != null, "Regeneration not found: " + regenerationRegen.toString());
            found = null;
        }
    }

    public static void showPlot(TransientSolution<DeterministicEnablingState, ?> rewards) {
        final JPanel plot = TransientSolutionViewer.solutionChart(rewards);
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                JFrame frame = new JFrame("Plot");
                frame.add(plot);
                frame.setDefaultCloseOperation(3);
                frame.setExtendedState(6);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }

    public static void showPlots(
            TransientSolution<DeterministicEnablingState, ?> solutionRegenerative,
            TransientSolution<DeterministicEnablingState, ?> solutionOneGen) {
        showPlot(solutionRegenerative);
        showPlot(solutionOneGen);
        try {
            Thread.sleep(100000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
