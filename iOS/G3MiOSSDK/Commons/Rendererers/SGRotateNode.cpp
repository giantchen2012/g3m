//
//  SGRotateNode.cpp
//  G3MiOSSDK
//
//  Created by Diego Gomez Deck on 11/8/12.
//
//

#include "SGRotateNode.hpp"

#include "Context.hpp"
#include "GL.hpp"
#include "Vector3D.hpp"

void SGRotateNode::prepareRender(const RenderContext* rc) {
  GL* gl = rc->getGL();

  gl->pushMatrix();
  gl->multMatrixf(MutableMatrix44D::createRotationMatrix(Angle::fromDegrees(_angle),
                                                         Vector3D(_x, _y, _z)));
}

void SGRotateNode::cleanUpRender(const RenderContext* rc) {
  GL* gl = rc->getGL();
  gl->popMatrix();
}
