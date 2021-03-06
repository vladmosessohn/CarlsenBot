/*
 * © 2020 Grama Nicolae, Ioniță Radu , Mosessohn Vlad, 322CA
 */

package com.carlsenbot.table;

import com.carlsenbot.pieces.*;
import com.carlsenbot.position.Move;
import com.carlsenbot.position.Position;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class Table {
    /*
     * All the pieces are stored in a matrix, as it can offer faster access
     * to the pieces.
     *
     * First row of the matrix [0][*] - white pieces
     * Second row of the matrix [1][*] - black pieces
     *
     * The convention for the pieces indexes is:
     * [*][0] - King
     * [*][1] - Queen
     * [*][2,3] - Rooks
     * [*][4,5] - Knights
     * [*][6,7] - Bishops
     * [*][8-15] - Pawns
     *
     * ! IMPORTANT !
     * The id's of the pieces are a bit different. Because the value of a empty
     * cell in the matrix of "positions" is 0, we need to use the 1 -> 16 range
     * for white pieces, respectively -1 -> -16 range for the black pieces.
     */

    private byte[][] positions;
    private byte blackID;
    private byte whiteID;
    private Piece[][] pieces;
    private ArrayList<Move> moveHistory;
    private CheckSystem checkSystem;
    private boolean isWhiteTurn;

    /**
     * Initialise an empty table
     */
    public Table() {
        isWhiteTurn = true;
        positions = new byte[8][8];
        blackID = -1;
        whiteID = 1;
        pieces = new Piece[2][16];
        moveHistory = new ArrayList<>();
        checkSystem = new CheckSystem(this);
    }

    public Table(Table other) {
        positions = new byte[8][8];
        pieces = new Piece[2][16];
        isWhiteTurn = other.isWhiteTurn;

        for (int i = 0; i < 8; i++) {
            System.arraycopy(other.positions[i], 0, positions[i], 0, 8);
        }

        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 16; j++) {
                if(other.pieces[i][j] != null) {
                    // This must be copy constructor
                    Piece p = other.pieces[i][j];
                    if (p instanceof King) {
                        pieces[i][j] = new King(p);
                    } else if (p instanceof Queen) {
                        pieces[i][j] = new Queen(p);
                    } else if (p instanceof Knight) {
                        pieces[i][j] = new Knight(p);
                    } else if (p instanceof Bishop) {
                        pieces[i][j] = new Bishop(p);
                    } else if (p instanceof Rook) {
                        pieces[i][j] = new Rook(p);
                    } else if (p instanceof Pawn) {
                        pieces[i][j] = new Pawn(p);
                    }
                    pieces[i][j].setId(p.getId());
                    pieces[i][j].setOnBoard(true);
                    pieces[i][j].setAssignedTable(this);
                }
            }
        }

        moveHistory = new ArrayList<>();
        blackID = other.blackID;
        whiteID = other.whiteID;
        for (Move historicMove : other.moveHistory) {
            moveHistory.add(new Move(historicMove));
        }
        checkSystem = new CheckSystem(this);
    }

    /**
     * Initialise a table using a matrix of pieces
     * The format of the matrix ([2][16]) is:
     * - First line - white pieces
     * - Second line - black pieces
     * @param pieces The matrix of pieces
     */
    public Table(Piece[][] pieces) {
        this();
        // Whites
        for (int i = 0; i < 16; ++i) {
            addPiece(pieces[0][i]);
            addPiece(pieces[1][i]);
        }
    }

    public byte[][] getPositions() { return positions; }
    public Piece[][] getPieces() { return pieces; }

    private void addMoveToHistory(Move move) {
        moveHistory.add(move);
    }

    /**
     * Check if a pawn can attack another pawn at this position using
     * "En Passant"
     * @param position The position of the pawn to be "en passanted"
     * @param myColor The color of the pawn that wants to capture
     * @return If the move is possible
     */
    public boolean canBeEnPassanted(Position position, PieceColor myColor) {
        Piece other = getPiece(position);
        if(moveHistory.size()  != 0) {
            Move lastMove = moveHistory.get(moveHistory.size() - 1);

            /*
             * To be able to do an "En Passant"
             * - the other piece must be a pawn
             * - the other pawn must be of a different color
             * - the other pawn must be the last moved piece
             * - he must have done his first move, a double move
             */
            return other instanceof Pawn &&
                    myColor != other.getColor() &&
                    other == lastMove.getPiece() &&
                    lastMove.getDistance() == 2d;
        }
        return false;
    }

    public PieceColor getTurnColor() {
        if(isWhiteTurn) {
            return PieceColor.White;
        } else {
            return PieceColor.Black;
        }
    }

    public ArrayList<Move> getMoveHistory() {
        return moveHistory;
    }

    public void setTurnColor(PieceColor color) {
        isWhiteTurn = color == PieceColor.White;
    }

    public void switchTurn() {
        isWhiteTurn = !isWhiteTurn;
    }

    /**
     * Add a piece to the table
     * @param piece The piece to add
     * @return If the piece could be added
     */
    public boolean addPiece(Piece piece) {
        Position p = piece.getPosition();
        if(isEmptyCell(p)) {
            // Add piece to the table, give it an ID, and then
            // save it in the piece matrix
            if(piece.isBlack()) {
                piece.setId(blackID);
                pieces[1][(blackID * -1) - 1] = piece;
                positions[p.getRow()][p.getCol()] = blackID--;
            } else {
                piece.setId(whiteID);
                pieces[0][whiteID - 1] = piece;
                positions[p.getRow()][p.getCol()] = whiteID++;
            }
            piece.setOnBoard(true);
            piece.setAssignedTable(this);
            return true;
        }
        return false;
    }

    /**
     * Check if specified player lost
     * @param color The color of the player
     * @return If he is in checkmate
     */
    public boolean checkmate(PieceColor color) {
        return checkSystem.kingIsInCheck(color) && getAllPossibleMoves(color).size() == 0;
    }

    /**
     * Check if the specified player is in stalemate
     * @param color The color of the player
     * @return If he is in stalemate
     */
    public boolean stalemate(PieceColor color) {
        return !checkSystem.kingIsInCheck(color) && getAllPossibleMoves(color).size() == 0;
    }

    public ArrayList<Move> getAllPossibleMoves(PieceColor color) {
        ArrayList<Move> moves = new ArrayList<>();

        Piece[] moveable;
        if(color == PieceColor.White) {
            moveable = pieces[0];
        } else {
            moveable = pieces[1];
        }

        for (Piece p : moveable) {
            if (p != null) {
                for (int i = 0; i < 8; i++) {
                    for (int j = 0; j < 8; j++) {
                        Position target = new Position(i, j);
                        Piece.MoveInfo info = p.isValidMove(target);
                        if(info.canMove) {
                            // Now check if the players king will be in check after this move
                            Table afterMove = new Table(this);
                            afterMove.movePiece(p.getPosition(), target);

                            // Check if king is in check
                            if(!afterMove.getCheckSystem().kingIsInCheck(isWhiteTurn ? PieceColor.White : PieceColor.Black)) {
                                moves.add(new Move(p.getPosition(), target, p));
                            }
                        }
                    }
                }
            }
        }
        return moves;
    }

    /**
     * Remove a piece from the table
     * @param piece The piece to remove
     * @return If the piece was found and removed
     */
    public boolean removePiece(Piece piece) {
        Position position = piece.getPosition();
        return removePiece(position);
    }

    /**
     * Remove a piece from a specific position from the table
     * The id's are not changed
     * @param position The position of the piece
     * @return If the piece was found and removed
     */
    public boolean removePiece(Position position) {
        // Check if the piece actually exist
        if(!isEmptyCell(position)) {
            Piece piece = getPiece(position);
            piece.setOnBoard(false);
            if(piece.isBlack()) {
                pieces[1][(piece.getId() * -1) - 1] = null;
            } else {
                pieces[0][(piece.getId()) - 1] = null;
            }
            positions[position.getRow()][position.getCol()] = 0;
            return true;
        }
        return false;
    }

    /**
     * Check if the pieces at the specified positions have the same color
     * Will return false when both cells are empty
     * @param source The first piece position
     * @param target The second piece position
     * @return If they have the same color
     */
    public boolean isSameColor(Position source, Position target) {
        // If they have the same sign, the product is greater than 0
        return idOfCell(target) * idOfCell(source) > 0;
    }

    /**
     * A wrapper for the normal "isSameColor", using pieces as parameters
     * @param p1 The first piece
     * @param p2 The second piece
     * @return If the two pieces have the same color
     */
    public boolean isSameColor(Piece p1, Piece p2) {
        return isSameColor(p1.getPosition(), p2.getPosition());
    }


    /**
     * Check if a cell is empty at the specified position
     * @param target The position to check
     * @return If the cell is empty
     */
    public boolean isEmptyCell(Position target) {
        return positions[target.getRow()][target.getCol()] == 0;
    }

    /**
     * Check if a cell is empty at the specified coordinates
     * @param row The row of the cell to check
     * @param col The column of the cell to check
     * @return If the cell is not empty
     */
    public boolean isNotEmptyCell(int row, int col) {
        return positions[row][col] != 0;
    }

    /**
     * Check if a cell is empty at the specified position
     * @param target The position of the target
     * @return If the cell is not empty
     */
    public boolean isNotEmptyCell(Position target) {
        return positions[target.getRow()][target.getCol()] != 0;
    }

    /**
     * Check if a cell is empty at the specified "chess coordinates"
     * @param target The chess coordinates to check
     * @return If the cell is empty
     */
    public boolean isEmptyCell(String target) {
        Position pos = new Position(target);
        return positions[pos.getRow()][pos.getCol()] == 0;
    }

    public int idOfCell(Position target) { return positions[target.getRow()][target.getCol()]; }

    /**
     * Move a piece to a position
     * @param start The position of the piece to be moved
     * @param target The position to move to
     * @return if the piece was moved
     */
    public boolean movePiece(Position start, Position target) {
        Piece piece = getPiece(start);
        if (piece != null) {
            Piece.MoveInfo info = piece.move(target);
            if (!info.canMove) {
                return false;
            }

            switchTurn();
            addMoveToHistory(new Move(start, target, piece));

            setCell(start, (byte) 0);
            setCell(target, piece.getId());
            return true;
        }
        return false;
    }

    public boolean movePiece(Piece piece, String destination) {
        Position target = new Position(destination);
        Position start = piece.getPosition();

        Piece.MoveInfo info = piece.move(target);
        if (!info.canMove) {
            return false;
        }

        switchTurn();
        addMoveToHistory(new Move(start, target, piece));

        setCell(start, (byte) 0);
        setCell(target, piece.getId());
        return true;
    }

    public void teleportRookCastle(Rook rook, Position destination) {
        setCell(rook.getPosition(), (byte) 0);
        setCell(destination, rook.getId());
        rook.setPosition(destination);
    }

    /**
     * Set the value of the cell to one specified
     * @param target The position of the cell
     * @param value The new value
     */
    private void setCell(Position target, byte value) {
        positions[target.getRow()][target.getCol()] = value;
    }

    /**
     * Returns a piece with a specific id
     * @param id The id of the piece
     * @return The desired piece
     */
    private Piece getPieceByID(int id) {
        if(id == 0) { return null; }
        else if(id > 0) { return pieces[0][id - 1];
        } else { return pieces[1][(-id) - 1]; }
    }

    /**
     * Returns the symbol of a piece with the specified id, that
     * is managed by the GameManager
     * @param id The id of the piece
     * @return The symbol of the desired piece
     */
    private String getSymbolOfPiece(int id) {
        Piece piece = getPieceByID(id);
        if (piece != null) {
            return piece.getSymbol();
        } else {
            return " ";
        }
    }

    /**
     * Return the piece placed at a specific position
     * @param position The position to search for the piece
     * @return The piece at the specified position
     */
    public Piece getPiece(Position position) {
        return getPieceByID(idOfCell(position));
    }

    /**
     * Return the contents of the positions matrix as a string
     * It contains the id's of the pieces
     * @return A string containing the position matrix
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; ++i) {
            for (int j = 0; j < 8; ++j) {
                sb.append(String.format("%3s", positions[i][j]));
            }
            sb.append("\n");
        }
        return sb.toString();
    }


    /**
     * Return a string containing the chess table, with all the pieces
     * @return The string
     */
    public String printTable() {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 8; ++i) {
            sb.append(8 - i);
            sb.append(" ");
            for (int j = 0; j < 8; ++j) {
                sb.append("[");
                sb.append(getSymbolOfPiece(positions[i][j]));
                sb.append("]");
            }
            sb.append("\n");
        }
        sb.append("   A  B  C  D  E  F  G  H\n");
        return sb.toString();
    }

    public CheckSystem getCheckSystem() {
        return checkSystem;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Table table = (Table) o;
        return blackID == table.blackID &&
                whiteID == table.whiteID &&
                isWhiteTurn == table.isWhiteTurn &&
                Arrays.equals(positions, table.positions) &&
                Arrays.equals(pieces, table.pieces) &&
                Objects.equals(moveHistory, table.moveHistory) &&
                checkSystem.equals(table.checkSystem);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(positions);
    }
}
