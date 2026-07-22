import { useState } from 'react';

export function useForm({ initialValues, validate, onSubmit }) {
  const [values, setValues] = useState(initialValues);
  const [errors, setErrors] = useState({});
  const [touched, setTouched] = useState({});
  const [submitting, setSubmitting] = useState(false);

  function handleChange(e) {
    const { name, value } = e.target;
    setValues((prev) => ({ ...prev, [name]: value }));
  }

  function handleBlur(e) {
    const { name } = e.target;
    setTouched((prev) => ({ ...prev, [name]: true }));
    if (validate) {
      setErrors(validate(values));
    }
  }

  async function handleSubmit(e) {
    if (e?.preventDefault) e.preventDefault();
    const fieldErrors = validate ? validate(values) : {};
    setErrors(fieldErrors);
    setTouched(
      Object.keys(values).reduce((acc, key) => {
        acc[key] = true;
        return acc;
      }, {})
    );
    if (Object.values(fieldErrors).some(Boolean)) return;

    setSubmitting(true);
    try {
      await onSubmit(values);
    } finally {
      setSubmitting(false);
    }
  }

  return { values, errors, touched, submitting, handleChange, handleBlur, handleSubmit, setValues };
}
