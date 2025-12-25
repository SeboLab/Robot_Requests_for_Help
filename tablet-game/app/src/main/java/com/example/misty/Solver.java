package com.example.misty;

import java.util.Random;

public class Solver {
    private final int ROWS;
    private final int COLUMNS;
    private final char[][] board;
    private final boolean[][] revealed;

    public Solver(int rows, int columns, char[][] board, boolean[][] revealed) {
        this.ROWS = rows;
        this.COLUMNS = columns;
        this.board = board;
        this.revealed = revealed;
    }

    public int[] getMove(String difficulty) {

        if (difficulty == null || difficulty.equals("NULL")) {
            return basicSolver();
        }
        else if (difficulty.equals("Medium")) {
            return mediumSolver();
        }
        else if (difficulty.equals("Hard")) {
            return hardSolver();
        }
        else {
            return basicSolver();
        }
        //switch (difficulty) {
           // case "Medium":
            //    return mediumSolver();
            //case "Hard":
            //    return hardSolver();
            //default:
            //    return basicSolver();
        //}
    }

    private int[] basicSolver() {
        Random random = new Random();
        int randRow, randCol;

        do {
            randRow = random.nextInt(ROWS);
            randCol = random.nextInt(COLUMNS);
        } while (revealed[randRow][randCol]); // Ensure it picks an unrevealed tile

        return new int[]{randRow, randCol};
    }

    private int[] mediumSolver() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                if (!revealed[row][col] && isSafe(row, col)) {
                    return new int[]{row, col};
                }
            }
        }
        return basicSolver(); // Fallback if no clear move found
    }

    private int[] hardSolver() {
        int bestRow = -1, bestCol = -1;
        int minRisk = Integer.MAX_VALUE;

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                if (!revealed[row][col]) {
                    int risk = calculateRisk(row, col);
                    if (risk < minRisk) {
                        minRisk = risk;
                        bestRow = row;
                        bestCol = col;
                    }
                }
            }
        }

        return (bestRow != -1 && bestCol != -1) ? new int[]{bestRow, bestCol} : basicSolver();
    }

    private boolean isSafe(int row, int col) {
        int safeCount = 0;
        int totalAdj = 0;

        for (int[] dir : new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
            int newRow = row + dir[0], newCol = col + dir[1];

            if (isValid(newRow, newCol)) {
                totalAdj++;
                if (revealed[newRow][newCol] && board[newRow][newCol] != 'B') {
                    safeCount++;
                }
            }
        }
        return totalAdj > 0 && safeCount == totalAdj; // Safe if all adjacent revealed tiles are safe
    }

    private int calculateRisk(int row, int col) {
        int risk = 0;

        for (int[] dir : new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
            int newRow = row + dir[0], newCol = col + dir[1];

            if (isValid(newRow, newCol) && revealed[newRow][newCol]) {
                if (board[newRow][newCol] == 'B') {
                    risk += 100; // High risk if adjacent to a bomb
                } else {
                    risk += Character.getNumericValue(board[newRow][newCol]); // Use number clue
                }
            }
        }
        return risk;
    }

    private boolean isValid(int row, int col) {
        return row >= 0 && row < ROWS && col >= 0 && col < COLUMNS;
    }
}
