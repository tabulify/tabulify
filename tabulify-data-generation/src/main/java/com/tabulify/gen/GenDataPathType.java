package com.tabulify.gen;

import net.bytle.type.MediaType;
import net.bytle.type.MediaTypeAbs;

public class GenDataPathType {

    static public MediaType DATA_GEN = new MediaTypeAbs() {
        @Override
        public String getSubType() {
            return "gen";
        }

        @Override
        public String getType() {
            return "relation";
        }

        @Override
        public boolean isContainer() {
            return false;
        }

        @Override
        public String getExtension() {
            return "--datagen.yml";
        }

    };

}
