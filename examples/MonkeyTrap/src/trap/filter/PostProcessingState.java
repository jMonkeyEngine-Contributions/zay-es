/*
 * $Id$
 *
 * Copyright (c) 2013 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package trap.filter;

import com.jme3.app.Application;
import com.jme3.post.FilterPostProcessor;
import com.simsilica.lemur.event.BaseAppState;


/**
 *
 *  @version   $Revision$
 *  @author    Paul Speed
 */
public class PostProcessingState extends BaseAppState
{
    private FilterPostProcessor fpp;

    private DropShadowFilter shadows;

    public PostProcessingState()
    {
    }

    @Override
    protected void initialize(Application app)
    {
        int numSamples = app.getContext().getSettings().getSamples();
        fpp = new FilterPostProcessor(app.getAssetManager());
        if( numSamples > 0 )
            fpp.setNumSamples(numSamples);

        shadows = new DropShadowFilter();
        fpp.addFilter(shadows);
    }

    @Override
    protected void cleanup(Application app)
    {
    }

    @Override
    protected void enable()
    {
        getApplication().getViewPort().addProcessor(fpp);
    }

    @Override
    protected void disable()
    {
        getApplication().getViewPort().removeProcessor(fpp);
    }

}
