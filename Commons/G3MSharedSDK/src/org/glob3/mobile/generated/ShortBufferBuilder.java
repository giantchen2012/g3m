package org.glob3.mobile.generated; 
//
//  ShortBufferBuilder.cpp
//  G3MiOSSDK
//
//  Created by Diego Gomez Deck on 1/19/13.
//
//

//
//  ShortBufferBuilder.hpp
//  G3MiOSSDK
//
//  Created by Diego Gomez Deck on 1/19/13.
//
//



//class IShortBuffer;

public class ShortBufferBuilder
{

  public final class ShortArrayList {
    private short[] _array;
    private int     _size;

    public ShortArrayList() {
      this(10);
    }

    public ShortArrayList(final int initialCapacity) {
      if (initialCapacity < 0) {
        throw new IllegalArgumentException("Capacity can't be negative: " + initialCapacity);
      }
      _array = new short[initialCapacity];
      _size = 0;
    }

    public int size() {
      return _size;
    }

    public short get(final int index) {
      return _array[index];
    }

    public void push_back(final short element) {
      ensureCapacity(_size + 1);
      _array[_size++] = element;
    }

    private void ensureCapacity(final int mincap) {
      if (mincap > _array.length) {
        final int newcap = ((_array.length * 3) >> 1) + 1;
        final short[] olddata = _array;
        _array = new short[newcap < mincap ? mincap : newcap];
        System.arraycopy(olddata, 0, _array, 0, _size);
      }
    }

  }

  protected final ShortArrayList _values = new ShortArrayList();


  public final void add(short value)
  {
    _values.push_back(value);
  }

  public final IShortBuffer create()
  {
    final int size = _values.size();
  
    IShortBuffer result = IFactory.instance().createShortBuffer(size);
  
    for (int i = 0; i < size; i++)
    {
      result.rawPut(i, _values.get(i));
    }
  
    return result;
  }

}