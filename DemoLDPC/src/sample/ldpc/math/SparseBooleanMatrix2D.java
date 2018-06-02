package sample.ldpc.math;


import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.NotStrictlyPositiveException;
import org.apache.commons.math3.exception.NumberIsTooLargeException;
import org.apache.commons.math3.exception.OutOfRangeException;


public class SparseBooleanMatrix2D {

    private final int rows;
    private final int columns;
    private final OpenIntToBoolHashMap entries;


    public SparseBooleanMatrix2D(int rowDimension, int columnDimension)
            throws NotStrictlyPositiveException, NumberIsTooLargeException, IllegalArgumentException {
        if (rowDimension == 0 || columnDimension == 0)
            throw new IllegalArgumentException("Matrix dimensions should be non-zero");
        long lRow = rowDimension;
        long lCol = columnDimension;
        if (lRow * lCol >= Integer.MAX_VALUE) {
            throw new NumberIsTooLargeException(lRow * lCol, Integer.MAX_VALUE, false);
        }
        this.rows = rowDimension;
        this.columns = columnDimension;
        this.entries = new OpenIntToBoolHashMap();
    }


    public SparseBooleanMatrix2D(boolean[] row) throws IllegalArgumentException {
        if (row.length == 0) throw new IllegalArgumentException("Boolean vector should be non-empty");
        rows = 1;
        columns = row.length;
        entries = new OpenIntToBoolHashMap();
        for (int i = 0; i < columns; i++)
            if (row[i]) set(0, i, true);
    }

    public SparseBooleanMatrix2D(boolean[][] matrix) throws IllegalArgumentException {
        if (matrix.length == 0 || matrix[0].length == 0)
            throw new IllegalArgumentException("Matrix dimensions should be non-zero");
        rows = matrix.length;
        columns = matrix[0].length;
        entries = new OpenIntToBoolHashMap();
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < columns; j++)
                if (matrix[i][j]) set(i, j, true);
    }


    public int getColumnDimension() {
        return columns;
    }


    public SparseBooleanMatrix2D multiply(SparseBooleanMatrix2D m)
            throws DimensionMismatchException, NumberIsTooLargeException {

        if (getColumnDimension() != m.getRowDimension()) {
            throw new DimensionMismatchException(getColumnDimension(), m.getRowDimension());
        }

        final int outCols = m.getColumnDimension();
        SparseBooleanMatrix2D out = new SparseBooleanMatrix2D(rows, outCols);
        for (OpenIntToBoolHashMap.Iterator iterator = entries.iterator(); iterator.hasNext();) {
            iterator.advance();
            final boolean value = iterator.value();
            final int key = iterator.key();
            final int i = key / columns;
            final int k = key % columns;
            for (int j = 0; j < outCols; ++j) {
                final int rightKey = m.computeKey(k, j);
                if (m.entries.containsKey(rightKey)) {
                    final int outKey = out.computeKey(i, j);
                    final boolean outValue =
                            out.entries.get(outKey) != (value && m.entries.get(rightKey));
                    if (outValue == false) {
                        out.entries.remove(outKey);
                    } else {
                        out.entries.put(outKey, outValue);
                    }
                }
            }
        }
        return out;
    }


    public boolean get(int row, int column) throws OutOfRangeException {
        checkRowIndex(row);
        checkColumnIndex(column);
        return entries.get(computeKey(row, column));
    }


    public int getRowDimension() {
        return rows;
    }

    public void set(int row, int column, boolean value)
            throws OutOfRangeException {
        checkRowIndex(row);
        checkColumnIndex(column);
        if (value == false) {
            entries.remove(computeKey(row, column));
        } else {
            entries.put(computeKey(row, column), value);
        }
    }


    private int computeKey(int row, int column) {
        return row * columns + column;
    }

    public void checkRowIndex(final int row) throws OutOfRangeException {
        if (row < 0 || row >= getRowDimension()) {
            throw new OutOfRangeException(row, 0, getRowDimension()-1);
        }
    }

    public void checkColumnIndex(final int column) throws OutOfRangeException {
        if (column < 0 || column >= getColumnDimension()) {
            throw new OutOfRangeException(column, 0, getColumnDimension()-1);
        }
    }

    public int rate() {
        return entries.size();
    }

    public boolean[][] toBooleans() {
        int rows = getRowDimension(), columns = getColumnDimension();
        boolean[][] result = new boolean[rows][columns];
        for (OpenIntToBoolHashMap.Iterator iterator = entries.iterator(); iterator.hasNext();) {
            iterator.advance();
            int key = iterator.key();
            result[key / columns][key % columns] = iterator.value();
        }
        return result;
    }

    public SparseBooleanMatrix2D transpose() {
        int rows = getRowDimension(), columns = getColumnDimension();
        SparseBooleanMatrix2D result = new SparseBooleanMatrix2D(columns, rows);
        for (OpenIntToBoolHashMap.Iterator iterator = entries.iterator(); iterator.hasNext();) {
            iterator.advance();
            int key = iterator.key();
            result.set(key % columns, key / columns, iterator.value());
        }
        return result;
    }

}
