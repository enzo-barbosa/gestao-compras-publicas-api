import { useId, useState } from 'react'

interface CampoDeSenhaProps {
  id: string
  label: string
  value: string
  onChange: (valor: string) => void
  placeholder?: string
  autoComplete: 'current-password' | 'new-password'
  minLength?: number
  maxLength?: number
  required?: boolean
}

export default function CampoSenha({
  id,
  label,
  value,
  onChange,
  placeholder,
  autoComplete,
  minLength,
  maxLength,
  required,
}: CampoDeSenhaProps) {
  const [visivel, setVisivel] = useState(false)
  const idBotao = useId()

  return (
    <div className="campo-senha">
      <label htmlFor={id}>{label}</label>
      <div className="campo-senha-linha">
        <input
          id={id}
          type={visivel ? 'text' : 'password'}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          placeholder={placeholder}
          autoComplete={autoComplete}
          minLength={minLength}
          maxLength={maxLength}
          required={required}
          aria-describedby={idBotao}
        />
        <button
          id={idBotao}
          type="button"
          className="btn-olho"
          aria-pressed={visivel}
          aria-label={visivel ? 'Ocultar senha' : 'Mostrar senha'}
          onClick={() => setVisivel((atual) => !atual)}
        >
          {visivel ? <IconeOlhoFechado /> : <IconeOlho />}
        </button>
      </div>
    </div>
  )
}

function IconeOlho() {
  return (
    <svg
      width="18"
      height="18"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z" />
      <circle cx="12" cy="12" r="3" />
    </svg>
  )
}

function IconeOlhoFechado() {
  return (
    <svg
      width="18"
      height="18"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-6.5 0-10-8-10-8a18.45 18.45 0 0 1 5.06-5.94" />
      <path d="M9.06 9.06a3 3 0 0 1 4.88 4.88" />
      <path d="M1 1l22 22" />
      <path d="M9.9 4.24A10.06 10.06 0 0 1 12 4c6.5 0 10 8 10 8a18.5 18.5 0 0 1-2.16 3.19" />
    </svg>
  )
}