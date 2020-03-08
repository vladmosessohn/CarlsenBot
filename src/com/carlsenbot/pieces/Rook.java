package com.carlsenbot.pieces;

import com.carlsenbot.position.Position;

public class Rook extends Piece {
    public Rook(PieceColor color, Position position, int id) {
        super(5d, color, position,"Rook", id);
    }

    @Override
    public String getSymbol() {
        if(isWhite()) {
            return unicodeToChar(9814);
        } else {
            return unicodeToChar(9820);
        }
    }
}