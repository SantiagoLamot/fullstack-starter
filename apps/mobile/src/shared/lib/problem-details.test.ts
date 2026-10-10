import { AxiosError, AxiosHeaders, type AxiosResponse } from 'axios';

import { applyProblemFieldErrors, getProblemDetails, type ProblemDetails } from './problem-details';

interface Form {
  email: string;
  fullName: string;
}

const problem: ProblemDetails = {
  title: 'Bad Request',
  status: 400,
  detail: 'Datos inválidos',
  errors: [
    { field: 'email', message: 'ya está en uso' },
    { field: 'fullName', message: 'no puede estar vacío' },
    { field: 'unknownField', message: 'error de otro campo' },
  ],
};

describe('getProblemDetails', () => {
  it('extrae el ProblemDetail del body de un error de axios', () => {
    const response = {
      data: problem,
      status: 400,
      statusText: 'Bad Request',
      headers: {},
      config: { headers: new AxiosHeaders() },
    } as AxiosResponse;
    const error = new AxiosError('400', 'ERR_BAD_REQUEST', undefined, undefined, response);

    expect(getProblemDetails(error)).toEqual(problem);
  });

  it('devuelve null si el body no es un ProblemDetail', () => {
    expect(getProblemDetails(new Error('network'))).toBeNull();
    expect(getProblemDetails({ foo: 'bar' })).toBeNull();
    expect(getProblemDetails({ status: 400, errors: [{ field: 1 }] })).toBeNull();
  });
});

describe('applyProblemFieldErrors', () => {
  it('setea los errores de campos conocidos y devuelve los no mapeados', () => {
    const setError = jest.fn();

    const unmapped = applyProblemFieldErrors<Form>(problem, setError, ['email', 'fullName']);

    expect(setError).toHaveBeenCalledTimes(2);
    expect(setError).toHaveBeenNthCalledWith(
      1,
      'email',
      { type: 'server', message: 'ya está en uso' },
      { shouldFocus: true },
    );
    expect(setError).toHaveBeenNthCalledWith(
      2,
      'fullName',
      { type: 'server', message: 'no puede estar vacío' },
      { shouldFocus: false },
    );
    expect(unmapped).toEqual([{ field: 'unknownField', message: 'error de otro campo' }]);
  });

  it('no hace nada sin problem', () => {
    const setError = jest.fn();
    expect(applyProblemFieldErrors<Form>(null, setError, ['email'])).toEqual([]);
    expect(setError).not.toHaveBeenCalled();
  });
});
