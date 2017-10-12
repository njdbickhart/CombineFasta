/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utils;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

/**
 *
 * @author dbickhart
 * @param <K>
 */
public class IndexedPriorityQueue<K extends Comparable<K>> implements Iterable<Integer> {
    private final int MaxNum;
    private int n;
    private final int[] pq;
    private final int[] qp;
    private  K[] keys;
    
    public IndexedPriorityQueue(int max){
        assert(max > 0);
        this.MaxNum = max;
        this.n = 0;
        keys = (K[]) new Comparable[MaxNum + 1];
        pq = new int[MaxNum + 1];
        qp = new int[MaxNum + 1];
        for(int i = 0; i <= MaxNum; i++)
            qp[i] = -1;
    }
    
    public boolean isEmpty(){
        return n == 0;
    }
    
    public int size(){
        return n;
    }
    
    public int minIndex(){
        assert(n != 0);
        return pq[1];
    }
    
    public K minKey(){
        assert(n != 0);
        return keys[pq[1]];
    }
    
    public void insert(int i, K key){
        assert(i > 0 && i < MaxNum && !contains(i));
        n++;
        qp[i] = n;
        pq[n] = i;
        keys[i] = key;
        swim(n);
    }
    
    private boolean greater(int i, int j){
        return keys[pq[i]].compareTo(keys[pq[j]]) > 0;
    }
    
    private void exchange(int i, int j){
        int swap = pq[i];
        pq[i] = pq[j];
        pq[j] = swap;
        qp[pq[i]] = i;
        qp[pq[j]] = j;
    }
    
    private void swim(int k){
        while(k > 1 && greater(k/2, k)){
            exchange(k, k/2);
            k = k/2;
        }
    }
    
    private void sink(int k){
        while(2*k <= n){
            int j = 2 *k;
            if(j < n && greater(j, j+1))
                j++;
            if(!greater(k, j))
                break;
            exchange(k, j);
            k = j;
        }
    }
    
    public int delMin(){
        assert(n != 0);
        int min = pq[1];
        exchange(1, n--);
        sink(1);
        assert(min == pq[n+1]);
        qp[min] = -1;
        keys[min] = null;
        pq[n+1] = -1;
        return min;
    }
    
    public K keyOf(int i){
        assert(i > 0 && i < MaxNum && contains(i));
        return keys[i];
    }
    
    public void changeKey(int i, K key){
        assert(i > 0 && i < MaxNum && contains(i));
        keys[i] = key;
        swim(qp[i]);
        sink(qp[i]);
    }
    
    public void decreaseKey(int i, K key){
        assert(i > 0 && i < MaxNum && contains(i));
        if(keys[i].compareTo(key) <= 0)
            return;
        keys[i] = key;
        swim(qp[i]);
    }
    
    public void increaseKey(int i, K key){
        assert(i > 0 && i < MaxNum && contains(i));
        if(keys[i].compareTo(key) >= 0)
            return;
        keys[i] = key;
        sink(qp[i]);
    }
    
    public void delete(int i){
        assert(i > 0 && i < MaxNum && contains(i));
        int index = qp[i];
        exchange(index, n--);
        swim(index);
        sink(index);
        keys[i] = null;
        qp[i] = -1;
    }
    
    public boolean contains(int i){
        assert(i >= 0 && i < MaxNum);
        return qp[i] != -1;
    }

    @Override
    public Iterator<Integer> iterator() {
        return new HeapIterator();
    }
    
    private class HeapIterator implements Iterator<Integer>{
        private IndexedPriorityQueue<K> copy;
        
        public HeapIterator(){
            copy = new IndexedPriorityQueue<>(pq.length - 1);
            for(int i = 1; i <= n; i++)
                copy.insert(pq[i], keys[pq[i]]);
        }

        @Override
        public boolean hasNext() {
            return !copy.isEmpty();
        }

        @Override
        public Integer next() {
            if(!hasNext()) throw new NoSuchElementException();
            return copy.delMin();
        }
        
    }

    @Override
    public void forEach(Consumer<? super Integer> cnsmr) {
        Iterable.super.forEach(cnsmr); //To change body of generated methods, choose Tools | Templates.
    }
    
}
