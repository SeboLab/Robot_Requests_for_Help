package com.example.misty;

public class Board {
    public int rows;
    public int columns;
    public Square[][] board;
    public int bombRow;
    public int bombColumn;
    public int goldRow;
    public int goldColumn;


    public Board(int rows, int columns) {
        this.rows = rows;
        this.columns = columns;
        this.board = new Square[rows][columns];
        int row = (int) (Math.random() * rows);
        int column = (int) (Math.random() * columns);
        board[row][column] = new Square('G');
        goldColumn = column;
        goldRow = row;
        System.out.println("gold row " + goldRow + "," + goldColumn);

        do {
            row =  (int) (Math.random() * rows);
            column =(int) (Math.random() * columns);
        } while (row == goldRow && column == goldColumn);
        board[row][column] = new Square('B');
        bombColumn = column;
        bombRow = row;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                if (board[i][j] == null) {
                    board[i][j] = new Square ((char) ('0' + distanceFromGold(i, j)));
                }
            }
        }
        printFullBoard();
    }

    public void printFullBoard() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                System.out.print(board[i][j].getValue() + " ");
            }
            System.out.println();
        }
    }
    public char passSquare(int row, int column) {
        return this.board[row][column].getValue();
    }
    public void printPlayerBoard() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                if (board[i][j].getVisible()) {
                    System.out.print(board[i][j].getValue() + " ");
                } else {
                    System.out.print("f ");
                }
            }
            System.out.println();
        }
    }
    public int distanceFromBomb (int row, int column) {
        int distance = Math.abs(row - bombRow) + Math.abs(column - bombColumn);
        System.out.println("Distance from bomb: " + distance);
        return distance;
    }
    public int distanceFromGold (int row, int column) {
        int distance = Math.abs(row - goldRow) + Math.abs(column - goldColumn);
        System.out.println("Distance from gold: " + distance);
        return distance;
    }
    public int playerMove (int row, int column) {
        if (board[row][column].isBomb()) {
            System.out.println("You hit a bomb! Game over!");
            return 0;
        } else if (board[row][column].isGold()) {
            System.out.println("You found the gold! You win!");
            return 1;
        } else {
            board[row][column].setVisible(true);
            return 2;
        }
    }


}

