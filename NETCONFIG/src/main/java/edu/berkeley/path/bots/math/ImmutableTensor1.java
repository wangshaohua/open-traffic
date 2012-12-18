package edu.berkeley.path.bots.math;

import java.util.Iterator;

import edu.berkeley.path.bots.netconfig.NetconfigException;


/**
 * An immutable vector (1-dimensional tensor).
 * 
 * @author tjhunter
 * 
 */
public class ImmutableTensor1 implements Iterable<Double> {
    private final double[] data_;

    public double get(int i) {
        return data_[i];
    }

    public int size() {
        return data_.length;
    }

    private ImmutableTensor1(double[] d) {
        data_ = d;
    }

    private static class ImmutableTensorIterator implements Iterator<Double> {
        private int pos;
        private double[] data;

        public ImmutableTensorIterator(double[] data) {
            this.pos = 0;
            this.data = data;
        }

        public boolean hasNext() {
            return pos < data.length;
        }

        public Double next() {
            double x = data[pos];
            pos += 1;
            return x;
        }

        public void remove() {
            throw new UnsupportedOperationException("!!");
        }
    }

    public Iterator<Double> iterator() {
        return new ImmutableTensorIterator(data_);
    }

    public static ImmutableTensor1 from(double[] d) throws NetconfigException {
        if (d == null) {
            throw new NetconfigException(null,
                    "data provided to immutable tensor is null");
        }
        return new ImmutableTensor1(d.clone());
    }

    public static ImmutableTensor1 fillWith(double d, int size) {
        double[] data = new double[size];
        for (int i = 0; i < size; ++i) {
            data[i] = d;
        }
        return new ImmutableTensor1(data);
    }

    /**
     * USE WITH CAUTION!!!.
     * 
     * @return
     */
    public double[] rawData() {
        return data_;
    }
}
