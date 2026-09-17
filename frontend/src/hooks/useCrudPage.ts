import { useCallback, useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import api from '../services/api'
import type { Pagina } from '../services/api'
import { EVENTO_ORG } from '../services/organizacoes'
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
  const [exclusao, setExclusao] = useState<{ item: T; mensagem: string } | null>(null)
  const [excluindo, setExcluindo] = useState(false)

  const paramEtros = useRef(config.params)
  const prevParamsJson = useRef(JSON.stringify(config.params))

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
    const serialized = JSON.stringify(config.params)
    paramEtros.current = config.params
    if (prevParamsJson.current !== serialized) {
      prevParamsJson.current = serialized
      void carregar()
    }
  }, [config.params, carregar])

  useEffect(() => {
    void carregar()
  }, [carregar])

  useEffect(() => {
    const atualizar = () => void carregar()
    window.addEventListener(EVENTO_ORG, atualizar)
    return () => window.removeEventListener(EVENTO_ORG, atualizar)
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

  function excluir(item: T) {
    setExclusao({ item, mensagem: config.confirmarExclusao(item) })
  }

  function cancelarExclusao() {
    setExclusao(null)
  }

  async function confirmarExclusao() {
    if (!exclusao) return
    setErro(null)
    setExcluindo(true)
    try {
      await api.delete(`${rota}/${exclusao.item.id}`)
      exibir('sucesso', config.mensagemExclusao)
      setExclusao(null)
      await carregar()
    } catch (e) {
      setErro(extrairMensagemErro(e))
      setExclusao(null)
    } finally {
      setExcluindo(false)
    }
  }

  return {
    itens,
    carregando,
    erro,
    setErro,
    form,
    setForm,
    editandoId,
    carregar,
    iniciarEdicao,
    cancelar,
    salvar,
    excluir,
    exclusao,
    cancelarExclusao,
    confirmarExclusao,
    excluindo,
  }
}