package ec.edu.epn.fis.aeis.auth.model.enums;

import lombok.Getter;

@Getter
public enum College {
    FC("Facultad de Ciencias"),
    FCA("Facultad de Ciencias Administrativas"),
    FICA("Facultad de Ingeniería Civil y Ambiental"),
    FIEE("Facultad de Ingeniería Eléctrica y Electrónica"),
    FGP("Facultad de Geología y Petróleos"),
    FIM("Facultad de Ingeniería Mecánica"),
    FIQA("Facultad de Ingeniería Química y Agroindustria"),
    FIS("Facultad de Ingeniería de Sistemas"),
    ESFOT("Escuela de Formación de Tecnólogos"),
    FB("Formación Básica");

    private final String description;

    College(String description) {
        this.description = description;
    }
}
