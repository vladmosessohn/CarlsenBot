/*
 * © 2020 Grama Nicolae, Ioniță Radu , Mosessohn Vlad, 322CA
 */

package com.carlsenbot.pieces;

import com.carlsenbot.main.GameManager;
import com.carlsenbot.position.Position;
import com.carlsenbot.table.Table;

public class Knight extends Piece {

    public Knight(PieceColor color, Position position) {
        super(30d, color, position,"Knight");
    }

    public Knight(PieceColor color, String position) {
        this(color, new Position(position));
    }

    public Knight(Piece other) {
        this(other.getColor(), new Position(other.getPosition()));
    }
    /*
     * Implementation of the get symbol
     */
    @Override
    public String getSymbol() {
        if(isWhite()) {
            return unicodeToChar(9816);
        } else {
            return unicodeToChar(9822);
        }
    }

    /*
     * Check if knight can move to the specified position
     */
    @Override
    public MoveInfo isValidMove(Position target) {
        MoveInfo info = new MoveInfo(target);
        // Every move is legal in forced mode

        Position source = getPosition();

        // If the target is the same cell
        if(source.getDistance(target) == 0) {
            return info;
        }

        if (assignedTable.isNotEmptyCell(target)) {
            if (isSameColor(target)) {
                return info;
            } else {
                info.setAttack();
            }
        }

        if((source.getDiffRow(target) == 2 && source.getDiffCol(target) == 1) || (source.getDiffRow(target) == 1 && source.getDiffCol(target) == 2)) {
            info.setMove();
        } else {
            info.setNotAttack();
        }

        return info;
    }
    @Override
    public MoveInfo move(Position target) {
        MoveInfo info = isValidMove(target);

        if (info.canMove) {
            if(info.attacking) {
                capturePiece(target);
            } else {
                movePiece(target);
            }
        }
        return info;
    }
}
