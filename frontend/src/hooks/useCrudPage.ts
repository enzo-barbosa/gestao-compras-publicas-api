import { useCallback, useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import api from '../services/api'
import type { Pagina } from '../services/api'
import { useToast } from '../context/useToast'
import { extrairMensagemErro } from '../utils/format'

export interface ConfiguracaoCrud<T extends { id: number }, F> {
  rota: string
  params?: Record<string, string | number>
  formVazio: F
  paraForm: (item: T) => F
  montarCorpo: (form: F, editandoId: number | null) => unknown
  aoSalvar?: (form: F, editandoId: number | null, corpo: unknown) => Promise<unknown>
  confirmarExclusao: (item: T) => string
  mensagemCriacao: string
  mensagemEdicao: string
  mensagemExclusao: string
}

export function useCrudPage<T extends { id: number }, F>(config: ConfiguracaoCrud<T, F>) {
  const { rota } = config
  const { exibir } = useToast()
  const [itens, setItens] = useState<T[]>([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState<string | null>(null)
  const [form, setForm] = useState<F>(config.formVazio)
  const [editandoId, setEditandoId] = useState<number | null>(null)

  const paramEtros = useRef(config.params)

  useEffect(() => {
    paramEtros.current = config.params
  }, [config.params])

  const carregar = useCallback(async () => {
    try {
      const resposta = await api.get<Pagina<T>>(rota, { params: paramEtros.current })
      setItens(resposta.data.content ?? [])
      setErro(null)
    } catch (e) {
      setErro(extrairMensagemErro(e))
    } finally {
      setCarregando(false)
    }
  }, [rota])

  useEffect(() => {
    void carregar()
  }, [carregar])

  function iniciarEdicao(item: T) {
    setEditandoId(item.id)
    setForm(config.paraForm(item))
  }

  function cancelar() {
    setEditandoId(null)
    setForm(config.formVazio)
  }

  async function salvar(evento: FormEvent) {
    evento.preventDefault()
    setErro(null)
    try {
      const corpo = config.montarCorpo(form, editandoId)
      if (config.aoSalvar) {
        await config.aoSalvar(form, editandoId, corpo)
      } else if (editandoId === null) {
        await api.post(rota, corpo)
        exibir('sucesso', config.mensagemCriacao)
      } else {
        await api.put(`${rota}/${editandoId}`, corpo)
        exibir('sucesso', config.mensagemEdicao)
      }
      cancelar()
      await carregar()
    } catch (e) {
      setErro(extrairMensagemErro(e))
    }
  }

  async function excluir(item: T) {
    if (!window.confirm(config.confirmarExclusao(item))) return
    setErro(null)
    try {
      await api.delete(`${rota}/${item.id}`)
      exibir('sucesso', config.mensagemExclusao)
      await carregar()
    } catch (e) {
      setErro(extrairMensagemErro(e))
    }
  }

  return { itens, carregando, erro, setErro, form, setForm, editandoId, carregar, iniciarEdicao, cancelar, salvar, excluir }
}