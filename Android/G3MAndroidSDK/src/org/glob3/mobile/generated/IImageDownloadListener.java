package org.glob3.mobile.generated; 
//
//  IBufferDownloadListener.h
//  G3MiOSSDK
//
//  Created by José Miguel S N on 26/06/12.
//  Copyright (c) 2012 __MyCompanyName__. All rights reserved.
//



public interface IImageDownloadListener
{

  void onDownload(URL url, IImage image);

  void onError(URL url);

  void onCancel(URL url);

  /* this method will be call, before onCancel, when the data arrived before the cancelation */
  void onCanceledDownload(URL url, IImage image);

}