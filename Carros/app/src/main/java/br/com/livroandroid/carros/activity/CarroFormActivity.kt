package br.com.livroandroid.carros.activity

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import br.com.livroandroid.carros.R
import br.com.livroandroid.carros.domain.Carro
import br.com.livroandroid.carros.domain.CarroService
import br.com.livroandroid.carros.domain.TipoCarro
import br.com.livroandroid.carros.domain.event.SaveCarroEvent
import br.com.livroandroid.carros.extensions.*
import br.com.livroandroid.carros.utils.CameraHelper
import kotlinx.android.synthetic.main.activity_carro_form.*
import kotlinx.android.synthetic.main.activity_carro_form_contents.*
import org.greenrobot.eventbus.EventBus
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

class CarroFormActivity : BaseActivity() {
    val camera = CameraHelper()
    // O Carro pode ser nulo no caso de um novo carro
    val carro: Carro? by lazy { intent.getParcelableExtra<Carro>("carro") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_carro_form)
        // Título da toolbar (nome do carro ou Novo Carro)
        setupToolbar(R.id.toolbar,
                carro?.nome ?: getString(R.string.novo_carro), true)
        // Atualiza os dados do formulário
        initViews()
        // Inicializa a camera
        camera.init(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Salva o estado do arquivo caso gire a tela
        camera.OnSaveInstanceState(outState)
    }

    // Inicializa as views
    fun initViews() {
        // Ao Clicar no header da foto abre a camera
        appBarImg.onClick { onClickAppBarImg() }
        // A função apply somente é executada se o objeto não for nulo
        carro?.apply {
            // Foto do  carro
            appBarImg.loadUrl(carro?.urlFoto)
            // Dados do carro
            tDesc.string = desc
            tNome.string = nome
            // Tipo do carro
            when (tipo) {
                "classicos" -> radioTipo.check(R.id.tipoClassico)
                "esportivos" -> radioTipo.check(R.id.tipoEsportivo)
                "luxo" -> radioTipo.check(R.id.tipoLuxo)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_form_carro, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.action_salvar -> taskSalvar()
        }
        return super.onOptionsItemSelected(item)

    }

    // Salva o Carro no WebService
    fun taskSalvar() {
        if (tNome.isEmpty()) {
            // Valida se o campo nome foi preenchido
            tNome.error = getString(R.string.msg_error_from_nome)
            return
        }
        if (tDesc.isEmpty()) {
            // Valida se o campo descrição foi preenchido
            tDesc.error = getString(R.string.msg_error_from_desc)
            return
        }
        val dialog = ProgressDialog.show(this, "Carros", "Salvando o carro, aguarde...",
                false, true)
        doAsync {
            // Cria um carro para Salvar/Atualizar
            val c = carro ?: Carro()
            // Copia valores do form para o carro
            c.nome = tNome.string
            c.desc = tDesc.string
            c.tipo = when (radioTipo.checkedRadioButtonId) {
                R.id.tipoClassico -> TipoCarro.classicos.name
                R.id.tipoEsportivo -> TipoCarro.esportivos.name
                else -> TipoCarro.luxo.name
            }
            // Se tiver foto, faz upload
            val file = camera.file
            if (file != null && file.exists()) {
                val response = CarroService.postFoto(file)
                if (response!!.isOK()) {
                    // Atualiza a URL da foto no carro
                    c.urlFoto = response.url
                }
            }
            // Salva o carro no servidor
            val response = CarroService.save(c)
            uiThread {
                // Mensagem com a resposta do servidor
                toast(response!!.msg)
                dialog.dismiss()
                finish()
                EventBus.getDefault().post(SaveCarroEvent(c))
            }
        }

    }

    // Ao clicar na imagem do AppHeader abre a camera
    fun onClickAppBarImg() {
        val ms = System.currentTimeMillis()
        // Nome do arquivo da foto
        val fileName = if (carro != null)
            "foto_carro_${carro?.id}.jpg" else "foto_carro_${ms}.jpg"
        // abre a camera
        val intent = camera.open(this, fileName)
        startActivityForResult(intent, 0)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            // Resize da imagem
            val bitmap = camera.getBitmap(600, 600)
            if (bitmap != null) {
                // Salva o arquivo neste tamanho
                camera.save(bitmap)
                // mostra a foto do carro
                appBarImg.setImageBitmap(bitmap)
            }
        }
    }
}
