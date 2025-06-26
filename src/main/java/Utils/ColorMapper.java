package Utils;

import catan.Catan;
import catan.PieceColor;

public class ColorMapper {
    public static PieceColor fromProtoColor(Catan.Color protoColor) {
        return PieceColor.valueOf(protoColor.name());
    }

    public static Catan.Color toProtoColor(PieceColor pieceColor) {
        return Catan.Color.valueOf(pieceColor.name());
    }
}
